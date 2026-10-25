package pe.utp.entrevistapro360.infrastructure.stt;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.utp.entrevistapro360.application.FalloEtapaException;
import pe.utp.entrevistapro360.application.port.out.TranscripcionSttPort;
@Component
public class OpenAiTranscripcionAdapter implements TranscripcionSttPort {
    private final String base,key;
    private final ObjectMapper json;
    private final Clock clock;
    private final Duration timeout;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    @org.springframework.beans.factory.annotation.Autowired
    public OpenAiTranscripcionAdapter(@Value("${integraciones.openai-base-url:https://api.openai.com}") String base,
            @Value("${integraciones.openai-api-key:}") String key, ObjectMapper json, Clock clock) {
        this(base,key,json,clock,Duration.ofSeconds(60));
    }
    OpenAiTranscripcionAdapter(String base,String key,ObjectMapper json,Clock clock,Duration timeout) {
        this.base=base; this.key=key; this.json=json; this.clock=clock; this.timeout=timeout;
    }
    @Override public String transcribir(Path wav, String modelo) {
        if (key.isBlank() || modelo==null || !modelo.matches("[a-zA-Z0-9._-]+"))
            throw new FalloEtapaException("CONFIGURACION_STT",false);
        String boundary="ep360-"+UUID.randomUUID();
        CuerpoAcotado receptor=new CuerpoAcotado();
        CompletableFuture<HttpResponse<byte[]>> solicitud=null;
        try {
            URI uri=URI.create(base.replaceAll("/+$","")+"/v1/audio/transcriptions");
            if (!java.util.Set.of("http","https").contains(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null)
                throw new IllegalArgumentException();
            var body=HttpRequest.BodyPublishers.concat(bytes("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\nContent-Type: audio/wav\r\n\r\n"),
                    HttpRequest.BodyPublishers.ofFile(wav), bytes("\r\n"+campo(boundary,"model",modelo)+campo(boundary,"language","es")
                            +campo(boundary,"response_format","json")+"--"+boundary+"--\r\n"));
            var request=HttpRequest.newBuilder(uri).timeout(timeout).header("Authorization","Bearer "+key)
                    .header("Content-Type","multipart/form-data; boundary="+boundary).POST(body).build();
            solicitud=http.sendAsync(request,info->receptor);
            // El plazo incluye la lectura del cuerpo, incluso si el proveedor ya envió cabeceras.
            var response=solicitud.get(timeout.toMillis(),TimeUnit.MILLISECONDS);
                int status=response.statusCode();
                if (status!=200) throw new FalloEtapaException("STT_HTTP_"+status,status==408 || status==429 || status>=500,
                        retryAfter(response.headers().firstValue("Retry-After").orElse("")));
                var node=json.readTree(response.body());
                String texto=node==null || !node.path("text").isTextual() ? null : node.get("text").asText();
                if (texto==null || texto.trim().isBlank()) throw new FalloEtapaException("TRANSCRIPCION_VACIA",false);
                return texto.trim();
        } catch (IllegalArgumentException ex) { throw new FalloEtapaException("CONFIGURACION_STT",false); }
        catch (TimeoutException ex) { throw new FalloEtapaException("STT_TIMEOUT",true); }
        catch (ExecutionException ex) {
            if (ex.getCause() instanceof FalloEtapaException fallo) throw fallo;
            throw new FalloEtapaException("STT_TRANSPORTE",true);
        }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new FalloEtapaException("STT_INTERRUMPIDO",true); }
        catch (IOException ex) { throw new FalloEtapaException("STT_TRANSPORTE",true); }
        finally { if(solicitud!=null && !solicitud.isDone()) solicitud.cancel(true); receptor.cancelar(); }
    }
    private static final class CuerpoAcotado implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> resultado=new CompletableFuture<>();
        private final ByteArrayOutputStream contenido=new ByteArrayOutputStream();
        private volatile Flow.Subscription subscription;
        public CompletionStage<byte[]> getBody() { return resultado; }
        public void onSubscribe(Flow.Subscription s) { subscription=s; s.request(1); }
        public void onNext(List<ByteBuffer> lote) {
            for(var b:lote) {
                if((long)contenido.size()+b.remaining()>2_000_000) {
                    cancelar();resultado.completeExceptionally(new FalloEtapaException("STT_RESPUESTA_INVALIDA",false));return;
                }
                byte[] bytes=new byte[b.remaining()];b.get(bytes);contenido.writeBytes(bytes);
            }
            subscription.request(1);
        }
        public void onError(Throwable error) { resultado.completeExceptionally(error); }
        public void onComplete() { resultado.complete(contenido.toByteArray()); }
        void cancelar() { var s=subscription;if(s!=null)s.cancel(); }
    }
    private HttpRequest.BodyPublisher bytes(String texto) { return HttpRequest.BodyPublishers.ofByteArray(texto.getBytes(StandardCharsets.UTF_8)); }
    private String campo(String boundary,String nombre,String valor) {
        return "--"+boundary+"\r\nContent-Disposition: form-data; name=\""+nombre+"\"\r\n\r\n"+valor+"\r\n";
    }
    private long retryAfter(String valor) {
        try { return Math.max(0,Long.parseLong(valor.trim())); }
        catch (NumberFormatException ex) {
            try { return Math.max(0,Duration.between(clock.instant(),ZonedDateTime.parse(valor,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).getSeconds()); }
            catch (java.time.format.DateTimeParseException invalid) { return 0; }
        }
    }
}

