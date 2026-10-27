package pe.utp.entrevistapro360.application.port.in;
import java.nio.file.Path;
import pe.utp.entrevistapro360.application.VistaRespuesta;
public interface GestionarRespuesta {
    VistaRespuesta confirmarRespuesta(long idUsuario, long idSesion, int idPregunta, Path archivoTemporal, long tamanoBytes);
    VistaRespuesta consultarRespuesta(long idUsuario, long idRespuesta);
    VistaRespuesta reintentarRespuesta(long idUsuario, long idRespuesta);
    pe.utp.entrevistapro360.application.ReproduccionVideo reproducirRespuesta(long idUsuario,long idRespuesta);
}
