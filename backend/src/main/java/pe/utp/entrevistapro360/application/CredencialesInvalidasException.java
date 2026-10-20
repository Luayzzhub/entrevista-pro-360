package pe.utp.entrevistapro360.application;

public final class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() { super("No pudimos iniciar sesión. Revisa tus credenciales o el estado de tu cuenta."); }
}
