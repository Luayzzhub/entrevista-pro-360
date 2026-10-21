package pe.utp.entrevistapro360.application;

public final class SesionActivaExistenteException extends RuntimeException {
    private final Long idSesionActiva;
    public SesionActivaExistenteException(Long idSesionActiva) {
        super("Ya tienes una sesión de práctica en curso. Continúala o finalízala antes de iniciar otra.");
        this.idSesionActiva = idSesionActiva;
    }
    public Long idSesionActiva() { return idSesionActiva; }
}
