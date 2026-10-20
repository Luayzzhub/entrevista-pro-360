package pe.utp.entrevistapro360.presentation;

import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import pe.utp.entrevistapro360.application.port.in.ConsultarCatalogo;
import pe.utp.entrevistapro360.presentation.dto.PreguntaResponse;
import pe.utp.entrevistapro360.presentation.dto.PuestoResponse;

@RestController
@RequestMapping("/api/v1/puestos")
public class CatalogoController {
    private final ConsultarCatalogo catalogo;
    public CatalogoController(ConsultarCatalogo catalogo) { this.catalogo = catalogo; }

    @GetMapping
    public List<PuestoResponse> listar() {
        return catalogo.listarPuestos().stream().map(PuestoResponse::from).toList();
    }

    @GetMapping("/{id}/preguntas")
    public List<PreguntaResponse> preguntas(@PathVariable @Positive int id) {
        return catalogo.listarPreguntas(id).stream().map(PreguntaResponse::from).toList();
    }
}
