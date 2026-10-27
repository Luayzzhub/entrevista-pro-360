package pe.utp.entrevistapro360.presentation.dto;

import java.math.BigDecimal;
import java.time.*;
import pe.utp.entrevistapro360.domain.SesionEntrevista;

public record PuntoEvolucionResponse(Long idSesion, OffsetDateTime fechaInicio, OffsetDateTime fechaFin,
                                    BigDecimal puntajePromedio, String versionRubrica) {
    public static PuntoEvolucionResponse from(SesionEntrevista s) {
        return new PuntoEvolucionResponse(s.idSesion(), s.fechaInicio().atOffset(ZoneOffset.UTC),
                s.fechaFin().atOffset(ZoneOffset.UTC), s.puntajePromedio(), "COM-1.0");
    }
}
