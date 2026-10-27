package pe.utp.entrevistapro360.domain;

import java.math.BigDecimal;
import java.util.List;

public record ComparacionHistorial(boolean habilitada, Integer idPuesto, String versionRubrica,
                                  int totalSesionesComparadas, BigDecimal promedioConjunto, BigDecimal mejorPuntaje,
                                  BigDecimal cambioVsPrimera, BigDecimal cambioVsAnterior, List<SesionEntrevista> serie) {
    public ComparacionHistorial { serie = List.copyOf(serie); }
}
