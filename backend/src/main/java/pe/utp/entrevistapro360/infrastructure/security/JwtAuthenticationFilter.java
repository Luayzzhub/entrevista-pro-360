package pe.utp.entrevistapro360.infrastructure.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.utp.entrevistapro360.application.port.out.UsuarioRepository;
import pe.utp.entrevistapro360.domain.Usuario;

/** Firma/tiempo y estado actual de la cuenta se comprueban en cada petición privada. */
public final class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtDecoder decoder;
    private final UsuarioRepository usuarios;
    private final AuthenticationEntryPoint errors;
    public JwtAuthenticationFilter(JwtDecoder decoder, UsuarioRepository usuarios, AuthenticationEntryPoint errors) {
        this.decoder = decoder; this.usuarios = usuarios; this.errors = errors;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/error") || path.equals("/api/v1/puestos") || path.startsWith("/api/v1/puestos/")
                || path.equals("/api/v1/auth") || path.startsWith("/api/v1/auth/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null) { chain.doFilter(request, response); return; }
        try {
            if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) throw new BadCredentialsException("Token inválido.");
            Jwt jwt = decoder.decode(authorization.substring(7));
            long id = Long.parseLong(jwt.getSubject());
            Usuario usuario = usuarios.buscarPorId(id).filter(Usuario::puedeAutenticarse)
                    .orElseThrow(() -> new BadCredentialsException("Cuenta no disponible."));
            if (id < 1 || !"POSTULANTE".equals(jwt.getClaimAsString("rol"))) throw new BadCredentialsException("Rol inválido.");
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(id, null,
                    List.of(new SimpleGrantedAuthority("ROLE_POSTULANTE"))));
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException | BadCredentialsException ex) {
            SecurityContextHolder.clearContext();
            errors.commence(request, response, new BadCredentialsException("Token inválido o cuenta no disponible."));
            return;
        }
        chain.doFilter(request, response);
    }
}
