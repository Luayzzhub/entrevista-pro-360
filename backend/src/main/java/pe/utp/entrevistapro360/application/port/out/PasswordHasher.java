package pe.utp.entrevistapro360.application.port.out;

public interface PasswordHasher {
    String hash(String password);
    boolean coincide(String password, String hash);
}
