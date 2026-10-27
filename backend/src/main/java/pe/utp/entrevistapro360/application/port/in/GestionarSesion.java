package pe.utp.entrevistapro360.application.port.in;

import pe.utp.entrevistapro360.application.ResumenSesionesUsuario;
import pe.utp.entrevistapro360.application.VistaSesion;
import pe.utp.entrevistapro360.domain.FiltroHistorial;

public interface GestionarSesion {
    VistaSesion iniciarSesion(long idUsuario, int idPuesto);
    VistaSesion consultarSesion(long idUsuario, long idSesion);
    VistaSesion cancelarSesion(long idUsuario, long idSesion);
    default ResumenSesionesUsuario consultarResumenUsuario(long idUsuario) { return consultarResumenUsuario(idUsuario, FiltroHistorial.predeterminado()); }
    ResumenSesionesUsuario consultarResumenUsuario(long idUsuario, FiltroHistorial filtro);
}
