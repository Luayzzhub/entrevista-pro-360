package pe.utp.entrevistapro360.domain;

import java.math.*;
import java.util.*;

public final class CalculadoraEvolucionHistorial {
    private CalculadoraEvolucionHistorial() { }

    /** Recibe el conjunto ya filtrado, siempre antes de paginar. PM-17. */
    public static ComparacionHistorial calcular(Integer idPuesto, String versionRubrica, List<RegistroHistorial> registros) {
        boolean habilitada = idPuesto != null && "COM-1.0".equals(versionRubrica);
        var serie = habilitada ? registros.stream().filter(RegistroHistorial::esComparable).map(RegistroHistorial::sesion)
                .filter(s -> idPuesto.equals(s.idPuesto()))
                .sorted(Comparator.comparing(SesionEntrevista::fechaFin).thenComparing(SesionEntrevista::idSesion)).toList()
                : List.<SesionEntrevista>of();
        int n = serie.size();
        if (n == 0) return new ComparacionHistorial(habilitada, idPuesto, versionRubrica, 0, null, null, null, null, serie);
        BigDecimal suma = serie.stream().map(SesionEntrevista::puntajePromedio).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mejor = serie.stream().map(SesionEntrevista::puntajePromedio).max(BigDecimal::compareTo).orElseThrow();
        BigDecimal ultimo = serie.get(n - 1).puntajePromedio();
        return new ComparacionHistorial(true, idPuesto, versionRubrica, n,
                suma.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP), mejor.setScale(2, RoundingMode.HALF_UP),
                n < 2 ? null : ultimo.subtract(serie.get(0).puntajePromedio()).setScale(2, RoundingMode.HALF_UP),
                n < 2 ? null : ultimo.subtract(serie.get(n - 2).puntajePromedio()).setScale(2, RoundingMode.HALF_UP), serie);
    }
}
