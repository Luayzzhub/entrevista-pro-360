package pe.utp.entrevistapro360.application;
import pe.utp.entrevistapro360.domain.*;
public record VistaRespuesta(RespuestaVideo respuesta, SesionEntrevista sesion, Puesto puesto, Pregunta pregunta,
        boolean yaExistia, boolean puedeReintentar, MetricaComunicacion metricas) { }
