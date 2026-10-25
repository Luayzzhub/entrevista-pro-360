package pe.utp.entrevistapro360.application.port.out;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import pe.utp.entrevistapro360.application.DatosProcesamiento;
import pe.utp.entrevistapro360.domain.*;
public interface ProcesamientoRepository {
    List<Long> candidatos(LocalDateTime ahora);
    <T> Optional<T> conRespuestaBloqueada(long id, Function<DatosProcesamiento,T> accion);
    void actualizar(RespuestaVideo respuesta);
    void actualizarSesion(SesionEntrevista sesion);
    Optional<Transcripcion> buscarTranscripcion(long respuesta);
    Optional<MetricaComunicacion> buscarMetrica(long respuesta);
    void guardarTranscripcion(Transcripcion transcripcion);
    void guardarMetrica(MetricaComunicacion metrica);
}
