package pe.utp.entrevistapro360.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import pe.utp.entrevistapro360.presentation.dto.ApiError;

/** El contrato de transporte también cubre errores anteriores al controlador. */
@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper;
    public SecurityExceptionHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        write(response, 401, "NO_AUTENTICADO", "Se requiere autenticación.");
    }
    @Override public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        write(response, 403, "ACCESO_DENEGADO", "No tienes acceso al recurso.");
    }
    private void write(HttpServletResponse response, int status, String codigo, String mensaje) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ApiError.of(codigo, mensaje));
    }
}
