package pe.utp.entrevistapro360.domain;
public final class SalidaEvaluacionInvalidaException extends RuntimeException {
    public SalidaEvaluacionInvalidaException() { super("La evaluación no cumple EVAL-1.0."); }
}

