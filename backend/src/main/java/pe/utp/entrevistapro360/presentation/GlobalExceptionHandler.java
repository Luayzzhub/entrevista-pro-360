package pe.utp.entrevistapro360.presentation;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.utp.entrevistapro360.application.PuestoNoEncontradoException;
import pe.utp.entrevistapro360.domain.CatalogoInvalidoException;
import pe.utp.entrevistapro360.presentation.dto.ApiError;
import pe.utp.entrevistapro360.domain.ValidacionUsuarioException;
import pe.utp.entrevistapro360.application.EmailYaRegistradoException;
import pe.utp.entrevistapro360.application.CredencialesInvalidasException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import pe.utp.entrevistapro360.application.SesionActivaExistenteException;
import pe.utp.entrevistapro360.application.SesionNoEncontradaException;
import pe.utp.entrevistapro360.domain.TransicionSesionInvalidaException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SesionActivaExistenteException.class)
    ResponseEntity<ApiError> sesionActiva(SesionActivaExistenteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("SESION_ACTIVA_EXISTENTE", ex.getMessage(),
                Map.of("id_sesion_activa", ex.idSesionActiva().toString())));
    }
    @ExceptionHandler(SesionNoEncontradaException.class)
    ResponseEntity<ApiError> sesionNoEncontrada(SesionNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("SESION_NO_ENCONTRADA", ex.getMessage()));
    }
    @ExceptionHandler(TransicionSesionInvalidaException.class)
    ResponseEntity<ApiError> sesionCompletada(TransicionSesionInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of("SESION_YA_COMPLETADA", ex.getMessage()));
    }

    @ExceptionHandler(ValidacionUsuarioException.class)
    ResponseEntity<ApiError> usuarioInvalido(ValidacionUsuarioException ex) {
        return ResponseEntity.badRequest().body(new ApiError("DATOS_INVALIDOS", ex.getMessage(), ex.erroresCampo()));
    }
    @ExceptionHandler(EmailYaRegistradoException.class)
    ResponseEntity<ApiError> emailDuplicado(EmailYaRegistradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("EMAIL_YA_REGISTRADO", ex.getMessage(), Map.of("email", ex.getMessage())));
    }
    @ExceptionHandler(CredencialesInvalidasException.class)
    ResponseEntity<ApiError> credencialesInvalidas(CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of("CREDENCIALES_INVALIDAS", ex.getMessage()));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> jsonInvalido(HttpMessageNotReadableException ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof UnrecognizedPropertyException campo) {
                // No reflejar nombres arbitrarios enviados por el cliente.
                return ResponseEntity.badRequest().body(new ApiError("CAMPOS_NO_PERMITIDOS", "La solicitud contiene campos no permitidos.",
                        Map.of("solicitud", "Envía únicamente los campos admitidos por esta operación.")));
            }
        }
        return ResponseEntity.badRequest().body(new ApiError("JSON_INVALIDO", "Envía un objeto JSON válido con los tipos de datos esperados.",
                Map.of("solicitud", "Revisa el formato del cuerpo JSON.")));
    }

    @ExceptionHandler(PuestoNoEncontradoException.class)
    ResponseEntity<ApiError> noEncontrado(PuestoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("PUESTO_NO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(CatalogoInvalidoException.class)
    ResponseEntity<ApiError> catalogoInvalido(CatalogoInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of("CATALOGO_INVALIDO", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> datosInvalidos(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        var snakeCase = new com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(snakeCase.translate(e.getField()), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("DATOS_INVALIDOS", "Revisa los campos indicados.", campos));
    }

    @ExceptionHandler({HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> parametroInvalido(Exception ex) {
        return ResponseEntity.badRequest().body(new ApiError("PARAMETRO_INVALIDO",
                "El identificador debe ser un número entero positivo.", Map.of("id", "Debe ser un entero positivo.")));
    }

    @ExceptionHandler(ErrorResponseException.class)
    ResponseEntity<ApiError> errorHttp(ErrorResponseException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ApiError.of("SOLICITUD_INVALIDA", "La solicitud no puede atenderse."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> inesperado(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse error) {
            return ResponseEntity.status(error.getStatusCode())
                    .body(ApiError.of("SOLICITUD_INVALIDA", "La solicitud no puede atenderse."));
        }
        LOG.error("Error no controlado al atender la solicitud", ex);
        return ResponseEntity.internalServerError().body(ApiError.of("ERROR_INTERNO", "No pudimos atender la solicitud."));
    }
}
