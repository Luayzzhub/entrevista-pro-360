package pe.utp.entrevistapro360.application.port.out;

import pe.utp.entrevistapro360.domain.Usuario;

public interface TokenProvider {
    Token emitir(Usuario usuario);
    record Token(String accessToken, long expiresIn) { }
}
