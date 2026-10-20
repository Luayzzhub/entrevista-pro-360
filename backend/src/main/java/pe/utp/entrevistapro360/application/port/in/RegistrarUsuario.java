package pe.utp.entrevistapro360.application.port.in;

import pe.utp.entrevistapro360.domain.Usuario;

public interface RegistrarUsuario {
    Usuario registrar(String nombreCompleto, String email, String password);
}
