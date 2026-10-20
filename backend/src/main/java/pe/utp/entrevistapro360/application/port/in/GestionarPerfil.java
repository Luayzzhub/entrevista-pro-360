package pe.utp.entrevistapro360.application.port.in;

import pe.utp.entrevistapro360.domain.Usuario;

public interface GestionarPerfil {
    Usuario consultar(long idUsuario);
    Usuario actualizar(long idUsuario, String nombreCompleto, String resumenPerfil);
}
