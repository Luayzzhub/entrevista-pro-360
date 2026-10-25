package pe.utp.entrevistapro360.application.port.out;

import java.util.*;
import java.util.function.*;
import pe.utp.entrevistapro360.domain.RespuestaVideo;
import pe.utp.entrevistapro360.domain.SesionEntrevista;

public interface SesionRepository {
    /** La transacción y el bloqueo viven hasta que termine el callback. */
    <T> T conUsuarioBloqueado(long idUsuario, Supplier<T> accion);
    <T> T conSesionBloqueada(long idUsuario, long idSesion, Function<SesionEntrevista, T> accion);
    <T> T enLectura(Supplier<T> accion);
    Optional<SesionEntrevista> buscarNoTerminal(long idUsuario);
    Optional<SesionEntrevista> buscarPropia(long idUsuario, long idSesion);
    SesionEntrevista guardarNueva(SesionEntrevista sesion);
    SesionEntrevista actualizar(SesionEntrevista sesion);
    List<SesionEntrevista> listarPropias(long idUsuario);
    List<RespuestaVideo> buscarRespuestas(Collection<Long> idsSesion);
    Optional<RespuestaVideo> buscarRespuesta(long idSesion, int idPregunta);
    Optional<RespuestaVideo> buscarRespuestaPropia(long idUsuario, long idRespuesta);
    RespuestaVideo guardarRespuesta(RespuestaVideo respuesta);
}
