package pe.utp.entrevistapro360.infrastructure.security;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import pe.utp.entrevistapro360.application.port.out.TokenProvider;
import pe.utp.entrevistapro360.domain.Usuario;

public final class JwtTokenProvider implements TokenProvider {
    public static final long DURACION_SEGUNDOS = 1800;
    public static final String ISSUER = "entrevista-pro-360";
    private final JwtEncoder encoder;
    private final Clock clock;
    public JwtTokenProvider(JwtEncoder encoder, Clock clock) { this.encoder = encoder; this.clock = clock; }
    @Override public Token emitir(Usuario usuario) {
        if (!usuario.puedeAutenticarse()) throw new IllegalArgumentException("La cuenta no puede emitir tokens.");
        var ahora = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        var claims = JwtClaimsSet.builder().issuer(ISSUER).subject(usuario.idUsuario().toString())
                .issuedAt(ahora).expiresAt(ahora.plusSeconds(DURACION_SEGUNDOS)).claim("rol", usuario.rol().name()).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new Token(token, DURACION_SEGUNDOS);
    }
}
