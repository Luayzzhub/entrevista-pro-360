package pe.utp.entrevistapro360.domain;

import java.time.*;
import java.util.Map;
import pe.utp.entrevistapro360.domain.SesionEntrevista.EstadoSesion;

public record FiltroHistorial(Integer idPuesto, LocalDate fechaDesde, LocalDate fechaHasta, EstadoSesion estado,
                              String versionRubrica, int page, int size) {
    public FiltroHistorial {
        if (idPuesto != null && idPuesto < 1) invalido("id_puesto", "El puesto debe ser un entero positivo.");
        if (page < 0) invalido("page", "La página debe ser mayor o igual a cero.");
        if (size < 1 || size > 50) invalido("size", "El tamaño de página debe estar entre 1 y 50.");
        if (versionRubrica != null && !"COM-1.0".equals(versionRubrica)) invalido("version_rubrica", "La única rúbrica admitida es COM-1.0.");
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            String mensaje = "La fecha inicial no puede ser posterior a la fecha final.";
            throw new FiltroHistorialInvalidoException(mensaje, Map.of("fecha_desde", mensaje, "fecha_hasta", mensaje));
        }
        // plusDays(1) debe ser representable incluso para entradas ISO extremas.
        if (LocalDate.MAX.equals(fechaHasta)) invalido("fecha_hasta", "La fecha final está fuera del rango admitido.");
    }
    private static void invalido(String campo, String mensaje) { throw new FiltroHistorialInvalidoException(mensaje, Map.of(campo, mensaje)); }
    public static FiltroHistorial predeterminado() { return new FiltroHistorial(null, null, null, null, null, 0, 10); }
    public boolean incluye(RegistroHistorial registro) {
        var s = registro.sesion();
        return (idPuesto == null || idPuesto.equals(s.idPuesto())) && (estado == null || estado == s.estadoSesion())
                && (fechaDesde == null || !s.fechaInicio().isBefore(inicioUtc(fechaDesde)))
                && (fechaHasta == null || s.fechaInicio().isBefore(inicioUtc(fechaHasta.plusDays(1))))
                && (versionRubrica == null || registro.esComparable());
    }
    private static LocalDateTime inicioUtc(LocalDate fecha) {
        return fecha.atStartOfDay(ZoneId.of("America/Lima")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
