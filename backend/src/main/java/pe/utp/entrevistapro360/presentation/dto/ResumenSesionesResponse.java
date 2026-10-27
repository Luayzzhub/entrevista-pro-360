package pe.utp.entrevistapro360.presentation.dto;

import java.util.List;
import pe.utp.entrevistapro360.application.ResumenSesionesUsuario;

public record ResumenSesionesResponse(SesionResumenResponse sesionPendiente, SesionResumenResponse ultimaSesionCompletada,
                                     long totalSesionesCompletadas, List<SesionResumenResponse> items,
                                     int pagina, int tamanoPagina, int totalItemsFiltrados, int totalPaginas,
                                     String avisoFiltroRubrica, ComparacionHistorialResponse comparacion,
                                     List<PuestoHistorialResponse> puestosHistorial) {
    public record PuestoHistorialResponse(Integer idPuesto, String nombrePuesto) { }
    public static ResumenSesionesResponse from(ResumenSesionesUsuario resumen) {
        return new ResumenSesionesResponse(SesionResumenResponse.from(resumen.sesionPendiente()), SesionResumenResponse.from(resumen.ultimaSesionCompletada()),
                resumen.totalSesionesCompletadas(), resumen.items().stream().map(SesionResumenResponse::from).toList(),
                resumen.pagina(), resumen.tamanoPagina(), resumen.totalItemsFiltrados(), resumen.totalPaginas(),
                resumen.avisoFiltroRubrica(), ComparacionHistorialResponse.from(resumen.comparacion(), resumen.nombrePuestoComparado()),
                resumen.puestosHistorial().stream().map(p -> new PuestoHistorialResponse(p.idPuesto(), p.nombrePuesto())).toList());
    }
}
