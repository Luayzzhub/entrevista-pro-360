package pe.utp.entrevistapro360.presentation.dto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import pe.utp.entrevistapro360.domain.Usuario;

/** Lista explícita de datos públicos: el hash nunca se serializa. */
public record UsuarioResponse(Long idUsuario, String email, String nombreCompleto,
                              Usuario.Rol rol, Usuario.Estado estado, OffsetDateTime fechaRegistro,
                              OffsetDateTime fechaActualizacion, String resumenPerfil) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.idUsuario(), usuario.email(), usuario.nombreCompleto(), usuario.rol(), usuario.estado(),
                usuario.fechaRegistro().atOffset(ZoneOffset.UTC),
                usuario.fechaActualizacion() == null ? null : usuario.fechaActualizacion().atOffset(ZoneOffset.UTC), usuario.resumenPerfil());
    }
}
