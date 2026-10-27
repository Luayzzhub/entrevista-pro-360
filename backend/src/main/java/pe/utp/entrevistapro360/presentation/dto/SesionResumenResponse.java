package pe.utp.entrevistapro360.presentation.dto;

import java.math.BigDecimal;
import java.time.*;
import pe.utp.entrevistapro360.application.ResumenSesionItem;
import pe.utp.entrevistapro360.domain.SesionEntrevista.EstadoSesion;

public record SesionResumenResponse(Long idSesion, Integer idPuesto, String nombrePuesto, String areaPuesto, String nivelExperiencia,
                                   EstadoSesion estadoSesion, BigDecimal puntajePromedio, OffsetDateTime fechaInicio, OffsetDateTime fechaFin,
                                   int totalPreguntas, int respuestasConfirmadas, int respuestasEvaluadas, Integer ordenPreguntaHabilitada,
                                   String versionRubrica, String motivoCierre) {
    public static SesionResumenResponse from(ResumenSesionItem item) {
        if (item == null) return null;
        var s = item.sesion(); var p = item.puesto();
        return new SesionResumenResponse(s.idSesion(), p.idPuesto(), p.nombrePuesto(), p.area(), p.nivelExperiencia(), s.estadoSesion(),
                s.puntajePromedio(), s.fechaInicio().atOffset(ZoneOffset.UTC), s.fechaFin() == null ? null : s.fechaFin().atOffset(ZoneOffset.UTC),
                3, item.respuestasConfirmadas(), item.respuestasEvaluadas(), item.ordenPreguntaHabilitada(), item.versionRubrica(), item.motivoCierre());
    }
}
