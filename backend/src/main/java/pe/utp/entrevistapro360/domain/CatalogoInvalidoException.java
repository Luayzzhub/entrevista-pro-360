package pe.utp.entrevistapro360.domain;

public final class CatalogoInvalidoException extends RuntimeException {
    public CatalogoInvalidoException() {
        super("El puesto debe tener exactamente tres preguntas activas con órdenes 1, 2 y 3.");
    }
}
