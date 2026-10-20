package pe.utp.entrevistapro360.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import pe.utp.entrevistapro360.application.port.in.GestionarPerfil;
import pe.utp.entrevistapro360.application.port.out.UsuarioRepository;
import pe.utp.entrevistapro360.domain.Usuario;

public final class GestionarPerfilService implements GestionarPerfil {
    private final UsuarioRepository usuarios;
    private final Clock clock;
    public GestionarPerfilService(UsuarioRepository usuarios, Clock clock) { this.usuarios = usuarios; this.clock = clock; }
    @Override public Usuario consultar(long idUsuario) {
        return usuarios.buscarPorId(idUsuario).filter(Usuario::puedeAutenticarse).orElseThrow(CredencialesInvalidasException::new);
    }
    @Override public Usuario actualizar(long idUsuario, String nombre, String resumen) {
        Usuario actualizado = consultar(idUsuario).actualizarPerfil(nombre, resumen, LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).withNano(0));
        return usuarios.actualizarPerfil(actualizado);
    }
}
