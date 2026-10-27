package pe.utp.entrevistapro360.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import pe.utp.entrevistapro360.domain.FiltroHistorial;
import pe.utp.entrevistapro360.domain.SesionEntrevista.EstadoSesion;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.utp.entrevistapro360.application.port.in.GestionarSesion;
import pe.utp.entrevistapro360.presentation.dto.*;

@RestController
@RequestMapping("/api/v1/sesiones")
public class SesionController {
    private final GestionarSesion sesiones;
    public SesionController(GestionarSesion sesiones) { this.sesiones = sesiones; }
    @PostMapping
    public ResponseEntity<SesionDetalleResponse> iniciar(Authentication authentication, @Valid @RequestBody CrearSesionRequest request) {
        var detalle = SesionDetalleResponse.from(sesiones.iniciarSesion((Long) authentication.getPrincipal(), request.idPuesto()));
        return ResponseEntity.created(URI.create("/api/v1/sesiones/" + detalle.idSesion())).body(detalle);
    }
    @GetMapping("/{id}")
    public SesionDetalleResponse consultar(Authentication authentication, @PathVariable @Positive long id) {
        return SesionDetalleResponse.from(sesiones.consultarSesion((Long) authentication.getPrincipal(), id));
    }
    @PostMapping("/{id}/cancelacion")
    public SesionDetalleResponse cancelar(Authentication authentication, @PathVariable @Positive long id,
                                           @RequestBody(required = false) CancelarSesionRequest request) {
        return SesionDetalleResponse.from(sesiones.cancelarSesion((Long) authentication.getPrincipal(), id));
    }
    @GetMapping
    public ResumenSesionesResponse resumen(Authentication authentication,
            @RequestParam(name="id_puesto", required=false) Integer idPuesto,
            @RequestParam(name="fecha_desde", required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(name="fecha_hasta", required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required=false) EstadoSesion estado,
            @RequestParam(name="version_rubrica", required=false) String versionRubrica,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) {
        return ResumenSesionesResponse.from(sesiones.consultarResumenUsuario((Long) authentication.getPrincipal(),
                new FiltroHistorial(idPuesto, fechaDesde, fechaHasta, estado, versionRubrica, page, size)));
    }
}
