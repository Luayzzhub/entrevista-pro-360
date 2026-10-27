package pe.utp.entrevistapro360.application;

import java.util.List;
import pe.utp.entrevistapro360.domain.*;

public record ResumenSesionesUsuario(ResumenSesionItem sesionPendiente, ResumenSesionItem ultimaSesionCompletada,
                                    long totalSesionesCompletadas, List<ResumenSesionItem> items,
                                    int pagina, int tamanoPagina, int totalItemsFiltrados, int totalPaginas,
                                    String avisoFiltroRubrica, ComparacionHistorial comparacion,
                                    String nombrePuestoComparado, List<Puesto> puestosHistorial) {
    public ResumenSesionesUsuario { items = List.copyOf(items); puestosHistorial = List.copyOf(puestosHistorial); }
}
