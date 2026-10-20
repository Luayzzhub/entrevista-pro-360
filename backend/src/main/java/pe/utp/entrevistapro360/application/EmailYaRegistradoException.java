package pe.utp.entrevistapro360.application;

public final class EmailYaRegistradoException extends RuntimeException {
    public EmailYaRegistradoException() { super("El correo ya está registrado."); }
}
