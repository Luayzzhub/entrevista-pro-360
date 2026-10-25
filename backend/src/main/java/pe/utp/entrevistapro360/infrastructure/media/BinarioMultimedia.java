package pe.utp.entrevistapro360.infrastructure.media;
import java.nio.file.*;
public final class BinarioMultimedia {
    private BinarioMultimedia() { }
    public static String resolver(String configurado) {
        if (!configurado.equals("ffprobe") && !configurado.equals("ffmpeg")) return configurado;
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; dir != null && i < 5; i++, dir = dir.getParent()) {
            Path portable = dir.resolve(".tools/ffmpeg/" + configurado + ".exe");
            if (Files.isRegularFile(portable)) return portable.toString();
        }
        return configurado;
    }
}
