package pe.utp.entrevistapro360.infrastructure.persistence;

import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.entrevistapro360.application.CredencialesInvalidasException;
import pe.utp.entrevistapro360.application.EmailYaRegistradoException;
import pe.utp.entrevistapro360.application.port.out.UsuarioRepository;
import pe.utp.entrevistapro360.domain.Usuario;

@Repository
@Transactional(readOnly = true)
public class UsuarioJpaAdapter implements UsuarioRepository {
    private final UsuarioJpaRepository usuarios;
    public UsuarioJpaAdapter(UsuarioJpaRepository usuarios) { this.usuarios = usuarios; }
    @Override public Optional<Usuario> buscarPorId(long id) { return usuarios.findById(id).map(UsuarioJpaEntity::toDomain); }
    @Override public Optional<Usuario> buscarPorEmail(String email) { return usuarios.findByEmail(email).map(UsuarioJpaEntity::toDomain); }
    @Override @Transactional public Usuario crear(Usuario usuario) {
        try { return usuarios.saveAndFlush(UsuarioJpaEntity.nuevo(usuario)).toDomain(); }
        catch (DataIntegrityViolationException ex) {
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause.getMessage() != null && cause.getMessage().toLowerCase(Locale.ROOT).contains("uk_usuarios_email")) {
                    throw new EmailYaRegistradoException();
                }
            }
            throw ex;
        }
    }
    @Override @Transactional public Usuario actualizarPerfil(Usuario usuario) {
        UsuarioJpaEntity entity = usuarios.findById(usuario.idUsuario()).orElseThrow(CredencialesInvalidasException::new);
        if (!entity.toDomain().puedeAutenticarse()) throw new CredencialesInvalidasException();
        entity.actualizarPerfil(usuario);
        return usuarios.saveAndFlush(entity).toDomain();
    }
}
