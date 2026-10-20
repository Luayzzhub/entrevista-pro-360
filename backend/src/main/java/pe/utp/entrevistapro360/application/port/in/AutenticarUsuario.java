package pe.utp.entrevistapro360.application.port.in;

import pe.utp.entrevistapro360.domain.Usuario;
import pe.utp.entrevistapro360.application.port.out.TokenProvider;

public interface AutenticarUsuario {
    Resultado autenticar(String email, String password);
    record Resultado(Usuario usuario, TokenProvider.Token token) { }
}
