package pe.utp.entrevistapro360.application;

import java.util.List;

public record ResumenSesionesUsuario(ResumenSesionItem sesionPendiente, ResumenSesionItem ultimaSesionCompletada,
                                    long totalSesionesCompletadas, List<ResumenSesionItem> items) {
    public ResumenSesionesUsuario { items = List.copyOf(items); }
}
