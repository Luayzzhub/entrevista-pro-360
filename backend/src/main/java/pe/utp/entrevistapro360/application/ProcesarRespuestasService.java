package pe.utp.entrevistapro360.application;

import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import pe.utp.entrevistapro360.application.port.out.*;
import pe.utp.entrevistapro360.domain.*;

public final class ProcesarRespuestasService {
    private final ProcesamientoRepository repo;
    private final AlmacenamientoVideoPort almacenamiento;
    private final ExtractorAudioPort extractor;
    private final TranscripcionSttPort stt;
    private final RenovacionLeasePort heartbeat;
    private final Clock clock;
    private final long base1, base2;
    private final CatalogoRepository catalogo;
    private final EvaluacionLlmPort llm;
    private final AtomicBoolean ocupado = new AtomicBoolean();
    private record Resultado(String audio, String texto, MetricaComunicacion metrica, EvaluacionSTAR evaluacion) {
        Resultado(String a,String t,MetricaComunicacion m) { this(a,t,m,null); }
    }
    public ProcesarRespuestasService(ProcesamientoRepository repo, AlmacenamientoVideoPort almacenamiento, ExtractorAudioPort extractor,
            TranscripcionSttPort stt, RenovacionLeasePort heartbeat, Clock clock, long base1, long base2) {
        this(repo,almacenamiento,extractor,stt,heartbeat,clock,base1,base2,null,(entrada,modelo)->{throw new FalloEtapaException("CONFIGURACION_LLM",false);});
    }
    public ProcesarRespuestasService(ProcesamientoRepository repo, AlmacenamientoVideoPort almacenamiento, ExtractorAudioPort extractor,
            TranscripcionSttPort stt, RenovacionLeasePort heartbeat, Clock clock, long base1, long base2, CatalogoRepository catalogo, EvaluacionLlmPort llm) {
        this.repo=repo; this.almacenamiento=almacenamiento; this.extractor=extractor; this.stt=stt; this.heartbeat=heartbeat;
        this.clock=clock; this.base1=base1; this.base2=base2;
        this.catalogo=catalogo;this.llm=llm;
    }
    private LocalDateTime ahora() { return LocalDateTime.now(clock).withNano(0); }
    public boolean procesarSiguiente() {
        if (!ocupado.compareAndSet(false,true)) return false;
        try {
            Optional<TrabajoReclamado> trabajo = reclamarSiguiente();
            trabajo.ifPresent(this::ejecutar);
            return trabajo.isPresent();
        } finally { ocupado.set(false); }
    }
    public Optional<TrabajoReclamado> reclamarSiguiente() {
        for (long id : repo.candidatos(ahora())) {
            Optional<TrabajoReclamado> claim = repo.conRespuestaBloqueada(id, datos -> {
                ContextoProcesamiento c = new ContextoProcesamiento(datos.respuesta());
                if (!c.elegible(datos.sesion(), ahora())) return null;
                if (c.agotado()) {
                    RespuestaVideo agotada = c.agotarLease(ahora(),base1,base2); repo.actualizar(agotada);
                    if (new ContextoProcesamiento(agotada).definitivo()) repo.actualizarSesion(datos.sesion().finalizarAnticipadamente(ahora()));
                    return null;
                }
                RespuestaVideo reclamada=c.reclamar(ahora()); repo.actualizar(reclamada);
                return new TrabajoReclamado(datos.sesion(),reclamada);
            });
            if (claim.isPresent()) return claim;
        }
        return Optional.empty();
    }
    public void ejecutar(TrabajoReclamado trabajo) {
        var contexto = new ContextoProcesamiento(trabajo.respuesta());
        try (var vigilancia = heartbeat.vigilar(() -> renovarLease(trabajo))) {
            Resultado resultado = switch (contexto.etapa()) {
                case EXTRAER_AUDIO -> extraer(trabajo,contexto);
                case TRANSCRIBIR -> transcribir(trabajo,contexto);
                case METRICAS -> metricas(trabajo);
                case EVALUAR -> evaluar(trabajo,contexto);
                case FINALIZADO -> throw new IllegalStateException("Un trabajo finalizado no es elegible.");
            };
            finalizar(trabajo,resultado);
        } catch (SalidaEvaluacionInvalidaException ex) {
            fallar(trabajo,new FalloEtapaException("SALIDA_EVALUACION_INVALIDA",true));
        } catch (TranscripcionSinPalabrasException ex) {
            fallar(trabajo,new FalloEtapaException("TRANSCRIPCION_SIN_PALABRAS",false));
        } catch (FalloEtapaException ex) {
            fallar(trabajo,ex);
        } catch (RuntimeException ex) {
            // Si la BD no permite confirmar el resultado, el lease persistido permite recuperar tras la caída.
            fallar(trabajo,new FalloEtapaException("PROCESAMIENTO_TEMPORAL",true));
        }
    }
    public void renovarLease(TrabajoReclamado trabajo) {
        repo.conRespuestaBloqueada(trabajo.respuesta().idRespuesta(), datos -> {
            var c = new ContextoProcesamiento(datos.respuesta());
            if (!vigente(trabajo,c)) return null;
            repo.actualizar(datos.sesion().esTerminal() ? c.liberar() : c.renovar(ahora()));
            return null;
        });
    }
    private boolean vigente(TrabajoReclamado trabajo, ContextoProcesamiento c) {
        return c.version()==trabajo.version() && c.etapa()==new ContextoProcesamiento(trabajo.respuesta()).etapa() && c.tieneLease();
    }
    private Resultado extraer(TrabajoReclamado trabajo, ContextoProcesamiento c) {
        if (c.audio()!=null && Set.of("AUDIO_EXTRAIDO","TRANSCRITO").contains(c.hito()) && almacenamiento.existePrivado(c.audio()))
            return new Resultado(c.audio(),null,null);
        Path video=null, audio=null;
        try {
            video=Files.createTempFile("ep360-proc-video-",".tmp"); audio=Files.createTempFile("ep360-proc-audio-",".wav");
            almacenamiento.descargarPrivado(trabajo.respuesta().publicIdCloud(),video);
            extractor.extraer(video,audio);
            String clave="usuarios/"+trabajo.sesion().idUsuario()+"/sesiones/"+trabajo.sesion().idSesion()+"/preguntas/"+trabajo.respuesta().idPregunta()+"/audio.wav";
            almacenamiento.subirAudioPrivado(clave,audio);
            return new Resultado(clave,null,null);
        } catch (IOException ex) { throw new FalloEtapaException("IO_TEMPORAL",true); }
        finally { eliminar(video); eliminar(audio); }
    }
    private Resultado transcribir(TrabajoReclamado trabajo, ContextoProcesamiento c) {
        var existente=repo.buscarTranscripcion(trabajo.respuesta().idRespuesta());
        if (existente.isPresent()) return new Resultado(null,validarTexto(existente.get().textoTranscrito()),null);
        if (c.audio()==null) throw new FalloEtapaException("AUDIO_NO_DISPONIBLE",false);
        Path audio=null;
        try {
            audio=Files.createTempFile("ep360-proc-stt-",".wav");
            almacenamiento.descargarPrivado(c.audio(),audio);
            return new Resultado(null,validarTexto(stt.transcribir(audio,c.modeloStt())),null);
        } catch (IOException ex) { throw new FalloEtapaException("IO_TEMPORAL",true); }
        finally { eliminar(audio); }
    }
    private String validarTexto(String texto) {
        if (texto==null || texto.trim().isBlank()) throw new FalloEtapaException("TRANSCRIPCION_VACIA",false);
        return texto.trim();
    }
    private Resultado metricas(TrabajoReclamado trabajo) {
        long id=trabajo.respuesta().idRespuesta();
        Transcripcion texto=repo.buscarTranscripcion(id).orElseThrow(()->new FalloEtapaException("TRANSCRIPCION_AUSENTE",false));
        var metrica=CalculadorMetricas.calcular(id,texto.textoTranscrito(),trabajo.respuesta().duracionSegundos(),ahora());
        if (metrica.palabrasPorMinuto().compareTo(new java.math.BigDecimal("9999.99"))>0)
            throw new FalloEtapaException("METRICAS_FUERA_DE_RANGO",false);
        return new Resultado(null,null,metrica);
    }
    private Resultado evaluar(TrabajoReclamado trabajo,ContextoProcesamiento contexto) {
        long id=trabajo.respuesta().idRespuesta();
        var t=repo.buscarTranscripcion(id).orElseThrow(()->new FalloEtapaException("PRERREQUISITOS_EVALUACION",false));
        var m=repo.buscarMetrica(id).orElseThrow(()->new FalloEtapaException("PRERREQUISITOS_EVALUACION",false));
        if(t.textoTranscrito()==null || t.textoTranscrito().isBlank() || !"MET-1.0".equals(m.versionMetricas()))
            throw new FalloEtapaException("PRERREQUISITOS_EVALUACION",false);
        var pregunta=catalogo.buscarPreguntasActivas(trabajo.sesion().idPuesto()).stream()
                .filter(p->p.idPregunta().equals(trabajo.respuesta().idPregunta())).findFirst()
                .orElseThrow(()->new FalloEtapaException("PRERREQUISITOS_EVALUACION",false));
        var existente=repo.buscarEvaluacion(id);
        if(existente.isPresent()) return new Resultado(null,null,null,ValidadorEvaluacion.validarGuardada(existente.get(),pregunta.aplicaStar()));
        var puesto=catalogo.buscarPuesto(trabajo.sesion().idPuesto()).orElseThrow(()->new FalloEtapaException("PRERREQUISITOS_EVALUACION",false));
        var entrada=new EntradaEvaluacion(puesto.nombrePuesto(),pregunta.enunciado(),pregunta.competenciaEvaluada(),pregunta.aplicaStar(),t.textoTranscrito());
        var e=ValidadorEvaluacion.validar(id,llm.evaluar(entrada,contexto.modeloLlm()),pregunta.aplicaStar(),ahora());
        return new Resultado(null,null,null,e);
    }
    private void finalizar(TrabajoReclamado trabajo, Resultado resultado) {
        repo.conRespuestaBloqueada(trabajo.respuesta().idRespuesta(), datos -> {
            var c=new ContextoProcesamiento(datos.respuesta());
            if (!vigente(trabajo,c)) return null;
            if (datos.sesion().esTerminal()) { repo.actualizar(c.liberar()); return null; }
            RespuestaVideo actualizada;
            switch (c.etapa()) {
                case EXTRAER_AUDIO -> actualizada=c.audioExtraido(resultado.audio());
                case TRANSCRIBIR -> {
                    if (repo.buscarTranscripcion(datos.respuesta().idRespuesta()).isEmpty())
                        repo.guardarTranscripcion(new Transcripcion(null,datos.respuesta().idRespuesta(),resultado.texto(),null,"es",ahora()));
                    actualizada=c.transcrito();
                }
                case METRICAS -> { repo.guardarMetrica(resultado.metrica()); actualizada=c.metricasCompletas(); }
                case EVALUAR -> { repo.guardarEvaluacion(resultado.evaluacion()); actualizada=c.evaluado(); }
                default -> { return null; }
            }
            repo.actualizar(actualizada); return null;
        });
    }
    private void fallar(TrabajoReclamado trabajo, FalloEtapaException error) {
        try {
            repo.conRespuestaBloqueada(trabajo.respuesta().idRespuesta(), datos -> {
                var c=new ContextoProcesamiento(datos.respuesta());
                if (!vigente(trabajo,c)) return null;
                if (datos.sesion().esTerminal()) { repo.actualizar(c.liberar()); return null; }
                RespuestaVideo fallida=c.fallar(error.codigo(),error.recuperable(),ahora(),error.retryAfter(),base1,base2);
                repo.actualizar(fallida);
                if (new ContextoProcesamiento(fallida).definitivo()) repo.actualizarSesion(datos.sesion().finalizarAnticipadamente(ahora()));
                return null;
            });
        } catch (RuntimeException noPersistido) {
            System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING,"No se pudo persistir el resultado; se conserva el lease para recuperación.");
        }
    }
    private void eliminar(Path archivo) {
        if (archivo==null) return;
        try { Files.deleteIfExists(archivo); }
        catch (IOException ex) { archivo.toFile().deleteOnExit(); }
    }
}
