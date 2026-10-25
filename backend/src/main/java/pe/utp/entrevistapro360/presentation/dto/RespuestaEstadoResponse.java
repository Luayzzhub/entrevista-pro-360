package pe.utp.entrevistapro360.presentation.dto;
import java.math.BigDecimal;
import java.time.*;
import pe.utp.entrevistapro360.application.VistaRespuesta;
public record RespuestaEstadoResponse(Long idRespuesta, Long idSesion, Integer idPregunta, int ordenPregunta,
        String enunciadoPregunta, String competenciaEvaluada, boolean aplicaStar, String nombrePuesto,
        String estadoSesion, String estadoProcesamiento, String etapaActual, String ultimoHitoValido,
        BigDecimal duracionSegundos, String formato, Long tamanoBytes, OffsetDateTime fechaGrabacion,
        boolean puedeReintentar, String error, ResumenMetricas metricas) {
    public record ResumenMetricas(int conteoPalabras, BigDecimal palabrasPorMinuto, int conteoMuletillas, String versionMetricas) { }
    public static RespuestaEstadoResponse from(VistaRespuesta vista) {
        var r = vista.respuesta();
        // El contexto y las claves privadas nunca se serializan al cliente.
        String error = r.estadoProcesamiento() == pe.utp.entrevistapro360.domain.RespuestaVideo.EstadoProcesamiento.FALLIDO
                ? (vista.puedeReintentar() ? "Se agotaron los intentos automáticos. Puedes reintentar el análisis una vez."
                   : vista.sesion().esTerminal() ? "Ocurrió una falla no recuperable. La sesión fue CANCELADA."
                   : "No se pudo completar el procesamiento de esta respuesta.") : null;
        var m=vista.metricas();
        return new RespuestaEstadoResponse(r.idRespuesta(), r.idSesion(), r.idPregunta(), vista.pregunta().orden(), vista.pregunta().enunciado(),
                vista.pregunta().competenciaEvaluada(), vista.pregunta().aplicaStar(), vista.puesto().nombrePuesto(),
                vista.sesion().estadoSesion().name(), r.estadoProcesamiento().name(),
                (String) r.procesamientoContexto().get("etapa_actual"), (String) r.procesamientoContexto().get("ultimo_hito_valido"),
                r.duracionSegundos(), r.formato(), r.tamanoBytes(), r.fechaGrabacion().atOffset(ZoneOffset.UTC), vista.puedeReintentar(), error,
                m==null ? null : new ResumenMetricas(m.conteoPalabras(),m.palabrasPorMinuto(),m.conteoMuletillas(),m.versionMetricas()));
    }
}
