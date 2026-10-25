package pe.utp.entrevistapro360.application.port.out;
import java.nio.file.Path;
import pe.utp.entrevistapro360.application.ResultadoAlmacenamiento;
public interface AlmacenamientoVideoPort {
    ResultadoAlmacenamiento asegurarObjetoPrivado(String publicIdCloud, Path archivo, long tamanoBytes, String contentType);
    void verificarObjetoPrivado(String publicIdCloud);
    void eliminarSilenciosamente(String publicIdCloud);
}
