package pe.utp.entrevistapro360.application;

import pe.utp.entrevistapro360.domain.Pregunta;
import pe.utp.entrevistapro360.domain.RespuestaVideo;
import pe.utp.entrevistapro360.domain.EvaluacionSTAR;

public record EstadoPreguntaSesion(Pregunta pregunta, RespuestaVideo respuesta, boolean habilitada,
        EvaluacionSTAR evaluacion,boolean puedeReintentar,String motivoFallo) { }
