package pe.utp.entrevistapro360.application;

import java.util.List;
import pe.utp.entrevistapro360.domain.*;

public record VistaSesion(SesionEntrevista sesion, Puesto puesto, List<EstadoPreguntaSesion> preguntas,
                          Pregunta preguntaHabilitada, int respuestasConfirmadas, int respuestasEvaluadas) {
    public VistaSesion { preguntas = List.copyOf(preguntas); }
}
