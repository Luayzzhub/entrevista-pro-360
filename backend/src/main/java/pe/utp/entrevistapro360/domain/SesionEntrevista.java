package pe.utp.entrevistapro360.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SesionEntrevista(Long idSesion, Long idUsuario, Integer idPuesto,
                              EstadoSesion estadoSesion, BigDecimal puntajePromedio,
                              LocalDateTime fechaInicio, LocalDateTime fechaFin) {
    public enum EstadoSesion { INICIADA, EN_PROCESO, COMPLETADA, CANCELADA }
}
