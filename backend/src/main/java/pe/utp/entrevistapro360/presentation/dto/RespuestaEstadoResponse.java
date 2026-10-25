package pe.utp.entrevistapro360.presentation.dto;
import java.math.BigDecimal;
import java.time.*;
import pe.utp.entrevistapro360.application.VistaRespuesta;
public record RespuestaEstadoResponse(Long idRespuesta, Long idSesion, Integer idPregunta, int ordenPregunta,
        String enunciadoPregunta, String competenciaEvaluada, boolean aplicaStar, String nombrePuesto,
        String estadoSesion, String estadoProcesamiento, String etapaActual, String ultimoHitoValido,
        BigDecimal duracionSegundos, String formato, Long tamanoBytes, OffsetDateTime fechaGrabacion,
        boolean puedeReintentar, String error) {
    public static RespuestaEstadoResponse from(VistaRespuesta vista) {
        var r = vista.respuesta();
        // Ningún proveedor/worker se ejecuta todavía. El contexto interno nunca se serializa al cliente.
        String error = r.estadoProcesamiento() == pe.utp.entrevistapro360.domain.RespuestaVideo.EstadoProcesamiento.FALLIDO
                ? "No se pudo completar el procesamiento de esta respuesta." : null;
        return new RespuestaEstadoResponse(r.idRespuesta(), r.idSesion(), r.idPregunta(), vista.pregunta().orden(), vista.pregunta().enunciado(),
                vista.pregunta().competenciaEvaluada(), vista.pregunta().aplicaStar(), vista.puesto().nombrePuesto(),
                vista.sesion().estadoSesion().name(), r.estadoProcesamiento().name(),
                (String) r.procesamientoContexto().get("etapa_actual"), (String) r.procesamientoContexto().get("ultimo_hito_valido"),
                r.duracionSegundos(), r.formato(), r.tamanoBytes(), r.fechaGrabacion().atOffset(ZoneOffset.UTC), false, error);
    }
}
