package pe.utp.entrevistapro360.domain;

public final class TransicionSesionInvalidaException extends RuntimeException {
    public TransicionSesionInvalidaException() { super("La sesión ya fue completada y no puede cancelarse."); }
}
