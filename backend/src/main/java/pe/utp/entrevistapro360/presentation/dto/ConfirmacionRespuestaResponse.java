package pe.utp.entrevistapro360.presentation.dto;
import java.math.BigDecimal;
import java.time.*;
import pe.utp.entrevistapro360.application.VistaRespuesta;
public record ConfirmacionRespuestaResponse(Long idRespuesta, Long idSesion, Integer idPregunta, int ordenPregunta,
        String estadoSesion, String estadoProcesamiento, String etapaActual, String ultimoHitoValido,
        BigDecimal duracionSegundos, String formato, Long tamanoBytes, OffsetDateTime fechaGrabacion, String rutaConsulta) {
    public static ConfirmacionRespuestaResponse from(VistaRespuesta vista) {
        var r = vista.respuesta();
        return new ConfirmacionRespuestaResponse(r.idRespuesta(), r.idSesion(), r.idPregunta(), vista.pregunta().orden(),
                vista.sesion().estadoSesion().name(), r.estadoProcesamiento().name(),
                (String) r.procesamientoContexto().get("etapa_actual"), (String) r.procesamientoContexto().get("ultimo_hito_valido"),
                r.duracionSegundos(), r.formato(), r.tamanoBytes(), r.fechaGrabacion().atOffset(ZoneOffset.UTC), "/api/v1/respuestas/" + r.idRespuesta());
    }
}
