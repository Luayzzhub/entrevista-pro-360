package pe.utp.entrevistapro360.support;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import pe.utp.entrevistapro360.infrastructure.media.BinarioMultimedia;
public final class MediaFixtures {
    private static final Path DIR;
    static {
        try { DIR = Files.createTempDirectory("ep360-test-media-"); }
        catch (IOException e) { throw new ExceptionInInitializerError(e); }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!DIR.toAbsolutePath().normalize().startsWith(Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize())) return;
            try (var files = Files.walk(DIR)) { for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); }
            catch (IOException ignored) { }
        }));
    }
    private MediaFixtures() { }
    public static synchronized Path archivo(String tipo) throws Exception {
        Path archivo = DIR.resolve(tipo + (tipo.equals("mp4") ? ".mp4" : ".webm"));
        if (Files.exists(archivo)) return archivo;
        if (tipo.equals("falso")) { Files.writeString(archivo, "Esto no es un video."); return archivo; }
        String duracion = tipo.equals("corto") ? "0.4" : tipo.equals("largo") ? "61" : "2";
        List<String> args = new ArrayList<>(List.of(BinarioMultimedia.resolver(System.getenv().getOrDefault("FFMPEG_BIN", "ffmpeg")),
                "-v", "error", "-nostdin", "-y", "-f", "lavfi", "-i", "testsrc=size=64x64:rate=10"));
        if (!tipo.equals("sin-audio")) args.addAll(List.of("-f", "lavfi", "-i", "sine=frequency=440:sample_rate=48000"));
        args.addAll(List.of("-t", duracion, "-threads", "1", "-filter_threads", "1", "-c:v", tipo.equals("mp4") ? "libx264" : tipo.equals("vp9") ? "libvpx-vp9" : "libvpx", "-pix_fmt", "yuv420p"));
        if (!tipo.equals("sin-audio")) args.addAll(List.of("-c:a", tipo.equals("mp4") ? "aac" : tipo.equals("codec-invalido") ? "libvorbis" : "libopus"));
        if (tipo.equals("streaming")) args.addAll(List.of("-live", "1", "-f", "webm"));
        args.add(archivo.toString());
        Path log = DIR.resolve(tipo + ".log");
        Process p = new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!p.waitFor(30, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new AssertionError("ffmpeg excedió el plazo."); }
        if (p.exitValue() != 0) throw new AssertionError("ffmpeg: " + Files.readString(log));
        return archivo;
    }
}
