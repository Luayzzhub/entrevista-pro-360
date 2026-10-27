package pe.utp.entrevistapro360.presentation.dto;

import java.math.BigDecimal;
import java.util.List;
import pe.utp.entrevistapro360.domain.ComparacionHistorial;

public record ComparacionHistorialResponse(boolean habilitada, Integer idPuesto, String nombrePuesto, String versionRubrica,
        int totalSesionesComparadas, BigDecimal promedioConjunto, BigDecimal mejorPuntaje, BigDecimal cambioVsPrimera,
        BigDecimal cambioVsAnterior, List<PuntoEvolucionResponse> serie) {
    public static ComparacionHistorialResponse from(ComparacionHistorial c, String nombre) {
        return new ComparacionHistorialResponse(c.habilitada(), c.idPuesto(), nombre, c.versionRubrica(), c.totalSesionesComparadas(),
                c.promedioConjunto(), c.mejorPuntaje(), c.cambioVsPrimera(), c.cambioVsAnterior(),
                c.serie().stream().map(PuntoEvolucionResponse::from).toList());
    }
}
