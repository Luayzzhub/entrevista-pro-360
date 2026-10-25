package pe.utp.entrevistapro360.infrastructure.media;

import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.utp.entrevistapro360.application.*;
import pe.utp.entrevistapro360.application.port.out.InspectorMultimediaPort;
import pe.utp.entrevistapro360.domain.RespuestaVideo;

@Component
public class FfprobeInspectorMultimediaAdapter implements InspectorMultimediaPort {
    private final String binario;
    private final ObjectMapper json;
    public FfprobeInspectorMultimediaAdapter(@Value("${integraciones.ffprobe-bin:ffprobe}") String binario, ObjectMapper json) {
        this.binario = BinarioMultimedia.resolver(binario); this.json = json;
    }
    @Override public MetadataMultimedia inspeccionar(Path archivo, long bytes) {
        if (bytes >= RespuestaVideo.MAXIMO_TAMANO_BYTES) throw new ArchivoDemasiadoGrandeException();
        if (bytes <= 0) throw new ArchivoMultimediaNoUtilizableException();
        try { if (Files.size(archivo) != bytes) throw new ArchivoMultimediaNoUtilizableException(); }
        catch (IOException ex) { throw new InspectorMultimediaNoDisponibleException(); }
        JsonNode info = ejecutar(archivo, "format=format_name,duration:stream=codec_type,codec_name,duration");
        List<JsonNode> videos = new ArrayList<>(), audios = new ArrayList<>();
        for (JsonNode stream : info.path("streams")) {
            if (stream.path("codec_type").asText().equals("video")) videos.add(stream);
            if (stream.path("codec_type").asText().equals("audio")) audios.add(stream);
        }
        if (videos.isEmpty() || audios.isEmpty()) throw new ArchivoMultimediaNoUtilizableException();
        String nombre = info.path("format").path("format_name").asText(), formato;
        if (nombre.contains("webm") && videos.stream().allMatch(s -> Set.of("vp8", "vp9").contains(s.path("codec_name").asText()))
                && audios.stream().allMatch(s -> s.path("codec_name").asText().equals("opus"))) formato = "webm";
        else if (nombre.contains("mp4") && videos.stream().allMatch(s -> s.path("codec_name").asText().equals("h264"))
                && audios.stream().allMatch(s -> s.path("codec_name").asText().equals("aac"))) formato = "mp4";
        else throw new FormatoMultimediaNoAdmitidoException();
        BigDecimal duracion = decimal(info.path("format").path("duration"));
        if (duracion == null) for (JsonNode stream : info.path("streams")) duracion = max(duracion, decimal(stream.path("duration")));
        if (duracion == null) {
            // WebM streaming de Chrome: sin cabecera de duración, pero con timestamps de paquetes.
            JsonNode paquetes = ejecutar(archivo, "packet=pts_time,duration_time");
            for (JsonNode paquete : paquetes.path("packets")) {
                BigDecimal pts = decimal(paquete.path("pts_time")), delta = decimal(paquete.path("duration_time"));
                if (pts != null) duracion = max(duracion, pts.add(delta == null ? BigDecimal.ZERO : delta));
            }
        }
        if (duracion == null || duracion.compareTo(BigDecimal.ONE) < 0 || duracion.compareTo(new BigDecimal("60")) > 0)
            throw new ArchivoMultimediaNoUtilizableException();
        return new MetadataMultimedia(formato, videos.get(0).path("codec_name").asText(), audios.get(0).path("codec_name").asText(), duracion, bytes);
    }
    private BigDecimal decimal(JsonNode node) {
        try { return new BigDecimal(node.asText()); } catch (NumberFormatException ex) { return null; }
    }
    private BigDecimal max(BigDecimal a, BigDecimal b) { return a == null ? b : b == null ? a : a.max(b); }
    private JsonNode ejecutar(Path archivo, String entradas) {
        Process proceso = null;
        ExecutorService lector = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "ffprobe-json"); t.setDaemon(true); return t; });
        try {
            proceso = new ProcessBuilder(binario, "-v", "error", "-show_entries", entradas, "-of", "json", archivo.toAbsolutePath().toString())
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            Process activo = proceso;
            Future<byte[]> salida = lector.submit(() -> {
                try (InputStream stream = activo.getInputStream()) {
                    byte[] datos = stream.readNBytes(8_000_001);
                    if (datos.length > 8_000_000) throw new FormatoMultimediaNoAdmitidoException();
                    return datos;
                }
            });
            if (!proceso.waitFor(15, TimeUnit.SECONDS)) throw new InspectorMultimediaNoDisponibleException();
            if (proceso.exitValue() != 0) throw new FormatoMultimediaNoAdmitidoException();
            try { return json.readTree(new String(salida.get(1, TimeUnit.SECONDS), StandardCharsets.UTF_8)); }
            catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new FormatoMultimediaNoAdmitidoException(); }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); throw new InspectorMultimediaNoDisponibleException();
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof FormatoMultimediaNoAdmitidoException formato) throw formato;
            throw new InspectorMultimediaNoDisponibleException();
        } catch (IOException | TimeoutException ex) { throw new InspectorMultimediaNoDisponibleException(); }
        finally { if (proceso != null && proceso.isAlive()) proceso.destroyForcibly(); lector.shutdownNow(); }
    }
}
