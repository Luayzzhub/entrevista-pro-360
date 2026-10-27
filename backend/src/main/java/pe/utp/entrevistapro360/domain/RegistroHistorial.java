package pe.utp.entrevistapro360.domain;

import java.util.*;
import pe.utp.entrevistapro360.domain.RespuestaVideo.EstadoProcesamiento;
import pe.utp.entrevistapro360.domain.SesionEntrevista.EstadoSesion;

/** Datos persistidos necesarios para D-10; no depende del banco activo actual. */
public record RegistroHistorial(SesionEntrevista sesion, List<RespuestaVideo> respuestas,
                                Map<Long, EvaluacionSTAR> evaluaciones) {
    public RegistroHistorial {
        respuestas = List.copyOf(respuestas);
        Map<Long, EvaluacionSTAR> propias = new HashMap<>();
        for (var r : respuestas) if (r.idRespuesta() != null && evaluaciones.containsKey(r.idRespuesta()))
            propias.put(r.idRespuesta(), evaluaciones.get(r.idRespuesta()));
        evaluaciones = Map.copyOf(propias);
    }

    public boolean esComparable() {
        if (sesion.estadoSesion() != EstadoSesion.COMPLETADA || sesion.puntajePromedio() == null
                || sesion.fechaFin() == null || respuestas.size() != 3) return false;
        Set<Long> ids = new HashSet<>(); Set<Integer> preguntas = new HashSet<>();
        for (var r : respuestas) {
            if (r.idRespuesta() == null || !ids.add(r.idRespuesta()) || !preguntas.add(r.idPregunta())
                    || !Objects.equals(sesion.idSesion(), r.idSesion()) || r.estadoProcesamiento() != EstadoProcesamiento.EVALUADO) return false;
            var e = evaluaciones.get(r.idRespuesta());
            if (e == null || !Objects.equals(e.idRespuesta(), r.idRespuesta()) || !"COM-1.0".equals(e.versionRubrica())) return false;
        }
        return true;
    }
}
