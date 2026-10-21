package pe.utp.entrevistapro360.application;

public final class SesionNoEncontradaException extends RuntimeException {
    public SesionNoEncontradaException() { super("La sesión solicitada no existe."); }
}
