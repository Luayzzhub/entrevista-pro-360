package pe.utp.entrevistapro360.presentation;
import jakarta.validation.constraints.Positive;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.utp.entrevistapro360.application.*;
import pe.utp.entrevistapro360.application.port.in.GestionarRespuesta;
import pe.utp.entrevistapro360.presentation.dto.*;
@RestController
public class RespuestaController {
    private final GestionarRespuesta respuestas;
    public RespuestaController(GestionarRespuesta respuestas) { this.respuestas = respuestas; }
    @PutMapping(value = "/api/v1/sesiones/{id}/preguntas/{idPregunta}/respuesta", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConfirmacionRespuestaResponse> confirmar(Authentication auth, @PathVariable @Positive long id,
            @PathVariable @Positive int idPregunta, @RequestPart("video") MultipartFile video) throws IOException {
        if (video.getSize() == 0) throw new VideoVacioException();
        if (video.getSize() >= pe.utp.entrevistapro360.domain.RespuestaVideo.MAXIMO_TAMANO_BYTES) throw new ArchivoDemasiadoGrandeException();
        Path temporal = Files.createTempFile("ep360-upload-", ".tmp");
        try {
            try (InputStream in = video.getInputStream()) { Files.copy(in, temporal, StandardCopyOption.REPLACE_EXISTING); }
            var respuesta = ConfirmacionRespuestaResponse.from(respuestas.confirmarRespuesta((Long) auth.getPrincipal(), id, idPregunta, temporal, video.getSize()));
            return ResponseEntity.accepted().location(URI.create(respuesta.rutaConsulta())).body(respuesta);
        } finally { Files.deleteIfExists(temporal); }
    }
    @GetMapping("/api/v1/respuestas/{id}")
    public RespuestaEstadoResponse consultar(Authentication auth, @PathVariable @Positive long id) {
        return RespuestaEstadoResponse.from(respuestas.consultarRespuesta((Long) auth.getPrincipal(), id));
    }
    public static class VideoVacioException extends RuntimeException { }
}
