package pe.utp.entrevistapro360.infrastructure.persistence;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import pe.utp.entrevistapro360.application.DatosProcesamiento;
import pe.utp.entrevistapro360.application.port.out.ProcesamientoRepository;
import pe.utp.entrevistapro360.domain.*;
@Repository @Transactional(readOnly=true)
public class ProcesamientoJpaAdapter implements ProcesamientoRepository {
    private final RespuestaVideoJpaRepository respuestas; private final SesionEntrevistaJpaRepository sesiones;
    private final TranscripcionJpaRepository transcripciones; private final MetricaJpaRepository metricas;
    private final EvaluacionJpaRepository evaluaciones;
    public ProcesamientoJpaAdapter(RespuestaVideoJpaRepository r,SesionEntrevistaJpaRepository s,TranscripcionJpaRepository t,MetricaJpaRepository m,EvaluacionJpaRepository e) { respuestas=r;sesiones=s;transcripciones=t;metricas=m;evaluaciones=e; }
    @Override public List<Long> candidatos(LocalDateTime ahora) {
        List<Long> ids=new ArrayList<>();
        for (int page=0;;page++) {
            var lote=respuestas.findCandidatas(PageRequest.of(page,100));
            for(var entity:lote) {
                RespuestaVideo r=entity.toDomain();
                if (!ContextoProcesamiento.compatible(r)) continue;
                var c=new ContextoProcesamiento(r);
                // La consulta excluye sesiones terminales; FINALIZADO no admite nuevos claims.
                if (c.etapa()!=ContextoProcesamiento.Etapa.FINALIZADO && c.leaseLibre(ahora)) {
                    Object proximo=r.procesamientoContexto().get("proximo_intento_utc");
                    if(proximo==null || !java.time.OffsetDateTime.parse(proximo.toString()).toInstant().isAfter(ahora.toInstant(java.time.ZoneOffset.UTC))) ids.add(r.idRespuesta());
                }
                if(ids.size()==32) return ids;
            }
            if(lote.size()<100) return ids;
        }
    }
    @Override @Transactional(isolation=Isolation.READ_COMMITTED)
    public <T> Optional<T> conRespuestaBloqueada(long id,Function<DatosProcesamiento,T> accion) {
        var sesionId=respuestas.findSesionId(id);
        if(sesionId.isEmpty()) return Optional.empty();
        // Orden único de locks: sesión y luego respuesta, también para heartbeat y reintento manual.
        var sesion=sesiones.findByIdForUpdate(sesionId.get());
        if(sesion.isEmpty()) return Optional.empty();
        return respuestas.findByIdForUpdate(id).map(r->accion.apply(new DatosProcesamiento(sesion.get().toDomain(),r.toDomain())));
    }
    @Override @Transactional public void actualizar(RespuestaVideo r) { var e=respuestas.findById(r.idRespuesta()).orElseThrow();e.actualizarProcesamiento(r);respuestas.saveAndFlush(e); }
    @Override @Transactional public void actualizarSesion(SesionEntrevista s) { var e=sesiones.findById(s.idSesion()).orElseThrow();e.actualizarEstado(s);sesiones.saveAndFlush(e); }
    @Override public Optional<Transcripcion> buscarTranscripcion(long r) { return transcripciones.findByRespuestaId(r).map(TranscripcionJpaEntity::toDomain); }
    @Override public Optional<MetricaComunicacion> buscarMetrica(long r) { return metricas.findByRespuestaId(r).map(MetricaJpaEntity::toDomain); }
    @Override @Transactional public void guardarTranscripcion(Transcripcion t) { if(transcripciones.findByRespuestaId(t.idRespuesta()).isEmpty())transcripciones.saveAndFlush(TranscripcionJpaEntity.nueva(t)); }
    @Override @Transactional public void guardarMetrica(MetricaComunicacion m) { var e=metricas.findByRespuestaId(m.idRespuesta()).orElseGet(()->MetricaJpaEntity.nueva(m));e.actualizar(m);metricas.saveAndFlush(e); }
    @Override public Optional<EvaluacionSTAR> buscarEvaluacion(long r) { return evaluaciones.findByRespuestaId(r).map(EvaluacionJpaEntity::toDomain); }
    @Override @Transactional public void guardarEvaluacion(EvaluacionSTAR e) {
        if(evaluaciones.findByRespuestaId(e.idRespuesta()).isEmpty()) evaluaciones.saveAndFlush(EvaluacionJpaEntity.nueva(e));
    }
    @Override public Map<Long,EvaluacionSTAR> buscarEvaluaciones(Collection<Long> ids) {
        if(ids.isEmpty())return Map.of();
        return evaluaciones.findByRespuestaIdIn(ids).stream().map(EvaluacionJpaEntity::toDomain)
                .collect(java.util.stream.Collectors.toMap(EvaluacionSTAR::idRespuesta,e->e));
    }
    @Override public List<ResultadoRespuestaSesion> buscarResultadosSesion(long sesion) {
        var rs=respuestas.findBySesionIdInOrderByIdAsc(List.of(sesion)).stream().map(RespuestaVideoJpaEntity::toDomain).toList();
        var ids=rs.stream().map(RespuestaVideo::idRespuesta).toList();
        if(ids.isEmpty())return List.of();
        var ts=transcripciones.findByRespuestaIdIn(ids).stream().map(TranscripcionJpaEntity::toDomain).collect(java.util.stream.Collectors.toMap(Transcripcion::idRespuesta,t->t));
        var ms=metricas.findByRespuestaIdIn(ids).stream().map(MetricaJpaEntity::toDomain).collect(java.util.stream.Collectors.toMap(MetricaComunicacion::idRespuesta,m->m));
        var es=buscarEvaluaciones(ids);
        return rs.stream().map(r->new ResultadoRespuestaSesion(r,ts.get(r.idRespuesta()),ms.get(r.idRespuesta()),es.get(r.idRespuesta()))).toList();
    }
}
