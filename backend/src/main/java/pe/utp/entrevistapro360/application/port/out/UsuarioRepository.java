package pe.utp.entrevistapro360.application.port.out;

import java.util.Optional;
import pe.utp.entrevistapro360.domain.Usuario;

public interface UsuarioRepository {
    Optional<Usuario> buscarPorId(long idUsuario);
    Optional<Usuario> buscarPorEmail(String emailNormalizado);
    Usuario crear(Usuario usuario);
    Usuario actualizarPerfil(Usuario usuario);
}
