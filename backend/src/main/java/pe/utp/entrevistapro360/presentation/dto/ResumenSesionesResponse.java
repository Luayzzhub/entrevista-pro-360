package pe.utp.entrevistapro360.presentation.dto;

import java.util.List;
import pe.utp.entrevistapro360.application.ResumenSesionesUsuario;

public record ResumenSesionesResponse(SesionResumenResponse sesionPendiente, SesionResumenResponse ultimaSesionCompletada,
                                     long totalSesionesCompletadas, List<SesionResumenResponse> items) {
    public static ResumenSesionesResponse from(ResumenSesionesUsuario resumen) {
        return new ResumenSesionesResponse(SesionResumenResponse.from(resumen.sesionPendiente()), SesionResumenResponse.from(resumen.ultimaSesionCompletada()),
                resumen.totalSesionesCompletadas(), resumen.items().stream().map(SesionResumenResponse::from).toList());
    }
}
