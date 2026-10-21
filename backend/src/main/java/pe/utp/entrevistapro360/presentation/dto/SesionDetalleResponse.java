package pe.utp.entrevistapro360.presentation.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import pe.utp.entrevistapro360.application.VistaSesion;
import pe.utp.entrevistapro360.domain.SesionEntrevista.EstadoSesion;

public record SesionDetalleResponse(Long idSesion, Long idUsuario, Integer idPuesto, String nombrePuesto, String areaPuesto,
                                   String nivelExperiencia, EstadoSesion estadoSesion, BigDecimal puntajePromedio,
                                   OffsetDateTime fechaInicio, OffsetDateTime fechaFin, int totalPreguntas,
                                   int respuestasConfirmadas, int respuestasEvaluadas, PreguntaResponse preguntaHabilitada,
                                   List<PreguntaSesionResponse> preguntas) {
    public static SesionDetalleResponse from(VistaSesion v) {
        var s = v.sesion(); var p = v.puesto();
        return new SesionDetalleResponse(s.idSesion(), s.idUsuario(), p.idPuesto(), p.nombrePuesto(), p.area(), p.nivelExperiencia(),
                s.estadoSesion(), s.puntajePromedio(), s.fechaInicio().atOffset(ZoneOffset.UTC),
                s.fechaFin() == null ? null : s.fechaFin().atOffset(ZoneOffset.UTC), v.preguntas().size(), v.respuestasConfirmadas(),
                v.respuestasEvaluadas(), v.preguntaHabilitada() == null ? null : PreguntaResponse.from(v.preguntaHabilitada()),
                v.preguntas().stream().map(PreguntaSesionResponse::from).toList());
    }
}
