package pe.utp.entrevistapro360.presentation;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.utp.entrevistapro360.application.port.in.GestionarPerfil;
import pe.utp.entrevistapro360.presentation.dto.ActualizarPerfilRequest;
import pe.utp.entrevistapro360.presentation.dto.UsuarioResponse;

@RestController
@RequestMapping("/api/v1/perfil")
public class PerfilController {
    private final GestionarPerfil perfil;
    public PerfilController(GestionarPerfil perfil) { this.perfil = perfil; }
    @GetMapping
    public UsuarioResponse consultar(Authentication authentication) {
        return UsuarioResponse.from(perfil.consultar((Long) authentication.getPrincipal()));
    }
    @PatchMapping
    public UsuarioResponse actualizar(Authentication authentication, @RequestBody ActualizarPerfilRequest request) {
        return UsuarioResponse.from(perfil.actualizar((Long) authentication.getPrincipal(), request.nombreCompleto(), request.resumenPerfil()));
    }
}
