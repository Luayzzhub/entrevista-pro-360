package pe.utp.entrevistapro360.application;

import pe.utp.entrevistapro360.domain.Pregunta;
import pe.utp.entrevistapro360.domain.RespuestaVideo;

public record EstadoPreguntaSesion(Pregunta pregunta, RespuestaVideo respuesta, boolean habilitada) { }
