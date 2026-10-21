package pe.utp.entrevistapro360.application.port.in;

import pe.utp.entrevistapro360.application.ResumenSesionesUsuario;
import pe.utp.entrevistapro360.application.VistaSesion;

public interface GestionarSesion {
    VistaSesion iniciarSesion(long idUsuario, int idPuesto);
    VistaSesion consultarSesion(long idUsuario, long idSesion);
    VistaSesion cancelarSesion(long idUsuario, long idSesion);
    ResumenSesionesUsuario consultarResumenUsuario(long idUsuario);
}
