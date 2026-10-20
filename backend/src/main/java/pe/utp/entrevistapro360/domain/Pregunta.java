package pe.utp.entrevistapro360.domain;

public record Pregunta(Integer idPregunta, Integer idPuesto, String enunciado,
                       String competenciaEvaluada, int orden, int tiempoLimiteSeg,
                       boolean activo, boolean aplicaStar) {
    public Pregunta {
        if (idPregunta == null || idPregunta < 1 || idPuesto == null || idPuesto < 1
                || enunciado == null || enunciado.isBlank()
                || competenciaEvaluada == null || competenciaEvaluada.isBlank()
                || orden < 1 || orden > 3 || tiempoLimiteSeg != 60) {
            throw new IllegalArgumentException("La pregunta requiere identidad, contenido, orden 1 a 3 y límite de 60 segundos.");
        }
    }
}
