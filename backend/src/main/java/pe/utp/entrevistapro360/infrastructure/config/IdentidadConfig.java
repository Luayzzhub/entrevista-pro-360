package pe.utp.entrevistapro360.infrastructure.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import pe.utp.entrevistapro360.application.*;
import pe.utp.entrevistapro360.application.port.in.*;
import pe.utp.entrevistapro360.application.port.out.*;
import pe.utp.entrevistapro360.infrastructure.security.*;

@Configuration
public class IdentidadConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean PasswordHasher passwordHasher(@Value("${seguridad.bcrypt.costo:12}") int costo) { return new BCryptPasswordHasher(costo); }

    @Bean SecretKey jwtSecretKey(@Value("${seguridad.jwt.secret:${JWT_SECRET:}}") String secret,
                                 @Value("${seguridad.jwt.permitir-clave-efimera:true}") boolean permitirEfimera) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 && permitirEfimera) { bytes = new byte[32]; new SecureRandom().nextBytes(bytes); }
        if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET debe contener al menos 32 bytes UTF-8.");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKey key, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        OAuth2TokenValidator<Jwt> claims = jwt -> {
            boolean valido = jwt.getIssuedAt() != null && jwt.getExpiresAt() != null && jwt.getSubject() != null
                    && jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond() == JwtTokenProvider.DURACION_SEGUNDOS
                    && jwt.getIssuedAt().isBefore(clock.instant().plusSeconds(1)) && clock.instant().isBefore(jwt.getExpiresAt());
            return valido ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps, new JwtIssuerValidator(JwtTokenProvider.ISSUER), claims));
        return decoder;
    }
    @Bean RegistrarUsuario registrarUsuario(UsuarioRepository usuarios, PasswordHasher passwords, Clock clock) {
        return new RegistrarUsuarioService(usuarios, passwords, clock);
    }
    @Bean TokenProvider tokenProvider(JwtEncoder encoder, Clock clock) { return new JwtTokenProvider(encoder, clock); }
    @Bean AutenticarUsuario autenticarUsuario(UsuarioRepository usuarios, PasswordHasher passwords, TokenProvider tokens) {
        return new AutenticarUsuarioService(usuarios, passwords, tokens);
    }
    @Bean GestionarPerfil gestionarPerfil(UsuarioRepository usuarios, Clock clock) { return new GestionarPerfilService(usuarios, clock); }
}
