package pe.utp.entrevistapro360.presentation.dto;

import pe.utp.entrevistapro360.domain.Pregunta;

public record PreguntaResponse(Integer idPregunta, Integer idPuesto, String enunciado,
                               String competenciaEvaluada, int orden, int tiempoLimiteSeg,
                               boolean activo, boolean aplicaStar) {
    public static PreguntaResponse from(Pregunta pregunta) {
        return new PreguntaResponse(pregunta.idPregunta(), pregunta.idPuesto(), pregunta.enunciado(),
                pregunta.competenciaEvaluada(), pregunta.orden(), pregunta.tiempoLimiteSeg(),
                pregunta.activo(), pregunta.aplicaStar());
    }
}
