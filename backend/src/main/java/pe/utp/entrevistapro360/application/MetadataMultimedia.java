package pe.utp.entrevistapro360.application;
import java.math.BigDecimal;
public record MetadataMultimedia(String contenedor, String codecVideo, String codecAudio, BigDecimal duracionSegundos, long tamanoBytes) {
    public String contentType() { return "video/" + contenedor; }
}
