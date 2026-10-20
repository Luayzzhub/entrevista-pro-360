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

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
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
