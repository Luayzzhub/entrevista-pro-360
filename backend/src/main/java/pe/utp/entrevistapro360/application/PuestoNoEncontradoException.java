package pe.utp.entrevistapro360.application;

public final class PuestoNoEncontradoException extends RuntimeException {
    public PuestoNoEncontradoException() {
        super("El puesto no existe o no está activo.");
    }
}
