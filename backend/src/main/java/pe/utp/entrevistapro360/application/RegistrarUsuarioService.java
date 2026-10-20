package pe.utp.entrevistapro360.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import pe.utp.entrevistapro360.application.port.in.RegistrarUsuario;
import pe.utp.entrevistapro360.application.port.out.PasswordHasher;
import pe.utp.entrevistapro360.application.port.out.UsuarioRepository;
import pe.utp.entrevistapro360.domain.Usuario;

public final class RegistrarUsuarioService implements RegistrarUsuario {
    private final UsuarioRepository usuarios;
    private final PasswordHasher passwords;
    private final Clock clock;
    public RegistrarUsuarioService(UsuarioRepository usuarios, PasswordHasher passwords, Clock clock) {
        this.usuarios = usuarios; this.passwords = passwords; this.clock = clock;
    }
    @Override public Usuario registrar(String nombre, String email, String password) {
        Usuario.validarRegistro(nombre, email, password);
        String normalizado = Usuario.normalizarEmail(email);
        if (usuarios.buscarPorEmail(normalizado).isPresent()) throw new EmailYaRegistradoException();
        Usuario usuario = Usuario.registrar(nombre, normalizado, passwords.hash(password), LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).withNano(0));
        return usuarios.crear(usuario);
    }
}
