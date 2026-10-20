package pe.utp.entrevistapro360.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.utp.entrevistapro360.application.port.out.PasswordHasher;

public final class BCryptPasswordHasher implements PasswordHasher {
    private final BCryptPasswordEncoder encoder;
    public BCryptPasswordHasher(int strength) { encoder = new BCryptPasswordEncoder(strength); }
    @Override public String hash(String password) { return encoder.encode(password); }
    @Override public boolean coincide(String password, String hash) { return encoder.matches(password, hash); }
}
