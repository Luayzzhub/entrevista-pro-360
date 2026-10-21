package pe.utp.entrevistapro360.presentation.dto;

import pe.utp.entrevistapro360.application.EstadoPreguntaSesion;
import pe.utp.entrevistapro360.domain.RespuestaVideo.EstadoProcesamiento;

public record PreguntaSesionResponse(Integer idPregunta, int orden, String enunciado, String competenciaEvaluada,
                                    int tiempoLimiteSeg, boolean aplicaStar, boolean habilitada, Long idRespuesta,
                                    EstadoProcesamiento estadoProcesamiento) {
    public static PreguntaSesionResponse from(EstadoPreguntaSesion e) {
        var p = e.pregunta(); var r = e.respuesta();
        return new PreguntaSesionResponse(p.idPregunta(), p.orden(), p.enunciado(), p.competenciaEvaluada(), p.tiempoLimiteSeg(),
                p.aplicaStar(), e.habilitada(), r == null ? null : r.idRespuesta(), r == null ? null : r.estadoProcesamiento());
    }
}
