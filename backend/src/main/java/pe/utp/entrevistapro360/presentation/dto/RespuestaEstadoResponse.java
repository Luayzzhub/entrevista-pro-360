package pe.utp.entrevistapro360.presentation.dto;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import pe.utp.entrevistapro360.domain.*;
import pe.utp.entrevistapro360.application.VistaRespuesta;
public record RespuestaEstadoResponse(Long idRespuesta, Long idSesion, Integer idPregunta, int ordenPregunta,
        String enunciadoPregunta, String competenciaEvaluada, boolean aplicaStar, String nombrePuesto,
        String estadoSesion, String estadoProcesamiento, String etapaActual, String ultimoHitoValido,
        BigDecimal duracionSegundos, String formato, Long tamanoBytes, OffsetDateTime fechaGrabacion,
        boolean puedeReintentar, String error, ResumenMetricas metricas, TranscripcionResponse transcripcion, EvaluacionResponse evaluacion) {
    public record ResumenMetricas(int conteoPalabras, BigDecimal palabrasPorMinuto, int conteoMuletillas, Map<String,Object> detalleMuletillas, String versionMetricas,OffsetDateTime fechaCalculo) { }
    public record TranscripcionResponse(String textoTranscrito,String idioma,OffsetDateTime fechaTranscripcion) { }
    public record CriterioResponse(BigDecimal puntaje,String justificacion) { }
    public record EvaluacionResponse(BigDecimal puntajeGeneral,String versionRubrica,String versionContrato,Map<String,CriterioResponse> criterios,
            boolean aplicaStar,Map<String,Object> detalleStar,String fortalezaPrincipal,String sintesisAreasMejora,String recomendaciones,OffsetDateTime fechaEvaluacion) {
        static EvaluacionResponse from(EvaluacionSTAR e) {
            if(e==null)return null;
            var justificaciones=(Map<?,?>)e.areasMejora().get("justificaciones");
            Map<String,CriterioResponse> criterios=new LinkedHashMap<>();
            var puntos=List.of(e.puntajeRelevancia(),e.puntajeClaridad(),e.puntajeEstructura(),e.puntajeEvidencias());
            for(int i=0;i<4;i++) {
                String k=ValidadorEvaluacion.CRITERIOS.get(i);criterios.put(k,new CriterioResponse(puntos.get(i),(String)justificaciones.get(k)));
            }
            return new EvaluacionResponse(e.puntajeGeneral(),e.versionRubrica(),(String)e.areasMejora().get("version_contrato"),criterios,e.aplicaStar(),e.detalleStar(),
                    e.fortalezaPrincipal(),(String)e.areasMejora().get("sintesis"),e.recomendaciones(),e.fechaEvaluacion().atOffset(ZoneOffset.UTC));
        }
    }
    public static RespuestaEstadoResponse from(VistaRespuesta vista) {
        var r = vista.respuesta();
        // El contexto y las claves privadas nunca se serializan al cliente.
        String error = r.estadoProcesamiento() == pe.utp.entrevistapro360.domain.RespuestaVideo.EstadoProcesamiento.FALLIDO
                ? (vista.puedeReintentar() ? "Se agotaron los intentos automáticos. Puedes reintentar el análisis una vez."
                   : vista.sesion().esTerminal() ? "Ocurrió una falla no recuperable. La sesión fue CANCELADA."
                   : "No se pudo completar el procesamiento de esta respuesta.") : null;
        var m=vista.metricas();var t=vista.transcripcion();
        return new RespuestaEstadoResponse(r.idRespuesta(), r.idSesion(), r.idPregunta(), vista.pregunta().orden(), vista.pregunta().enunciado(),
                vista.pregunta().competenciaEvaluada(), vista.pregunta().aplicaStar(), vista.puesto().nombrePuesto(),
                vista.sesion().estadoSesion().name(), r.estadoProcesamiento().name(),
                (String) r.procesamientoContexto().get("etapa_actual"), (String) r.procesamientoContexto().get("ultimo_hito_valido"),
                r.duracionSegundos(), r.formato(), r.tamanoBytes(), r.fechaGrabacion().atOffset(ZoneOffset.UTC), vista.puedeReintentar(), error,
                m==null ? null : new ResumenMetricas(m.conteoPalabras(),m.palabrasPorMinuto(),m.conteoMuletillas(),m.detalleMuletillas(),m.versionMetricas(),m.fechaCalculo().atOffset(ZoneOffset.UTC)),
                t==null ? null : new TranscripcionResponse(t.textoTranscrito(),t.idioma(),t.fechaTranscripcion().atOffset(ZoneOffset.UTC)),EvaluacionResponse.from(vista.evaluacion()));
    }
}
