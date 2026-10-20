package pe.utp.entrevistapro360.application;

import pe.utp.entrevistapro360.application.port.in.AutenticarUsuario;
import pe.utp.entrevistapro360.application.port.out.*;
import pe.utp.entrevistapro360.domain.Usuario;

public final class AutenticarUsuarioService implements AutenticarUsuario {
    private final UsuarioRepository usuarios;
    private final PasswordHasher passwords;
    private final TokenProvider tokens;
    private final String hashFicticio;
    public AutenticarUsuarioService(UsuarioRepository usuarios, PasswordHasher passwords, TokenProvider tokens) {
        this.usuarios = usuarios; this.passwords = passwords; this.tokens = tokens;
        this.hashFicticio = passwords.hash("credencial-inexistente-de-comparacion");
    }
    @Override public Resultado autenticar(String email, String password) {
        String normalizado = Usuario.normalizarEmail(email);
        Usuario.validarPassword(password);
        Usuario usuario = usuarios.buscarPorEmail(normalizado).orElse(null);
        boolean coincide = passwords.coincide(password, usuario == null ? hashFicticio : usuario.passwordHash());
        if (usuario == null || !coincide || !usuario.puedeAutenticarse()) throw new CredencialesInvalidasException();
        return new Resultado(usuario, tokens.emitir(usuario));
    }
}
