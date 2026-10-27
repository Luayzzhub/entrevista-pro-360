package pe.utp.entrevistapro360.application;

import java.nio.file.Path;
import java.time.*;
import java.util.List;
import pe.utp.entrevistapro360.application.port.in.GestionarRespuesta;
import pe.utp.entrevistapro360.application.port.out.*;
import pe.utp.entrevistapro360.domain.*;

public final class GestionarRespuestaService implements GestionarRespuesta {
    private final SesionRepository sesiones;
    private final CatalogoRepository catalogo;
    private final ProcesamientoRepository procesamiento;
    private final FirmaVideoPort firma;
    private final InspectorMultimediaPort inspector;
    private final AlmacenamientoVideoPort almacenamiento;
    private final Clock clock;
    private final String modeloStt, modeloLlm;
    public GestionarRespuestaService(SesionRepository sesiones, CatalogoRepository catalogo, InspectorMultimediaPort inspector,
            AlmacenamientoVideoPort almacenamiento, Clock clock, String modeloStt, String modeloLlm, ProcesamientoRepository procesamiento, FirmaVideoPort firma) {
        this.sesiones = sesiones; this.catalogo = catalogo; this.inspector = inspector;
        this.almacenamiento = almacenamiento; this.clock = clock; this.modeloStt = modeloStt; this.modeloLlm = modeloLlm;
        this.procesamiento = procesamiento;
        this.firma = firma;
    }
    @Override public VistaRespuesta confirmarRespuesta(long usuario, long sesion, int pregunta, Path archivo, long bytes) {
        VistaRespuesta previa = sesiones.conSesionBloqueada(usuario, sesion, s -> comprobar(s, pregunta));
        if (previa != null) return previa;
        MetadataMultimedia m = inspector.inspeccionar(archivo, bytes); // Sin transacción ni lock de MySQL.
        String clave = "usuarios/" + usuario + "/sesiones/" + sesion + "/preguntas/" + pregunta + "/respuesta." + m.contenedor();
        ResultadoAlmacenamiento objeto = almacenamiento.asegurarObjetoPrivado(clave, archivo, m.tamanoBytes(), m.contentType());
        try {
            VistaRespuesta resultado = sesiones.conSesionBloqueada(usuario, sesion, s -> {
                VistaRespuesta existente = comprobar(s, pregunta);
                if (existente != null) return existente;
                // HEAD breve: una compensación concurrente no debe dejar una referencia a un objeto eliminado.
                almacenamiento.verificarObjetoPrivado(clave);
                RespuestaVideo nueva = RespuestaVideo.confirmarSubida(s.idSesion(), pregunta, clave, m.duracionSegundos(),
                        m.contenedor(), m.tamanoBytes(), LocalDateTime.now(clock).withNano(0), modeloStt, modeloLlm);
                RespuestaVideo guardada = sesiones.guardarRespuesta(nueva);
                SesionEntrevista actualizada = sesiones.actualizar(s.registrarRespuestaConfirmada());
                return vista(guardada, actualizada, false);
            });
            if (objeto.creado() && !resultado.respuesta().publicIdCloud().equals(clave)) compensar(usuario, sesion, pregunta, clave);
            return resultado;
        } catch (RuntimeException fallo) {
            if (objeto.creado()) compensar(usuario, sesion, pregunta, clave);
            throw fallo;
        }
    }
    private void compensar(long usuario, long sesion, int pregunta, String clave) {
        try {
            sesiones.conSesionBloqueada(usuario, sesion, s -> {
                if (sesiones.buscarRespuesta(sesion, pregunta).filter(r -> r.publicIdCloud().equals(clave)).isEmpty())
                    almacenamiento.eliminarSilenciosamente(clave);
                return null;
            });
        } catch (RuntimeException noSePudoComprobar) {
            // Un commit incierto no permite borrar a ciegas. El reintento reconcilia la misma clave.
        }
    }
    private VistaRespuesta comprobar(SesionEntrevista s, int idPregunta) {
        List<Pregunta> preguntas = catalogo.buscarPreguntasActivas(s.idPuesto());
        Puesto puesto = catalogo.buscarPuesto(s.idPuesto()).orElseThrow(PuestoNoEncontradoException::new);
        puesto.validarPreguntasActivas(preguntas);
        if (preguntas.stream().noneMatch(p -> p.idPregunta() == idPregunta)) throw new PreguntaNoEncontradaException();
        var existente = sesiones.buscarRespuesta(s.idSesion(), idPregunta);
        if (existente.isPresent()) return vista(existente.get(), s, true);
        if (s.esTerminal()) throw new SesionNoAdmiteRespuestasException();
        Pregunta habilitada = s.determinarPreguntaHabilitada(preguntas, sesiones.buscarRespuestas(List.of(s.idSesion())))
                .orElseThrow(PreguntaNoHabilitadaException::new);
        if (habilitada.idPregunta() != idPregunta) throw new PreguntaNoHabilitadaException();
        return null;
    }
    @Override public VistaRespuesta consultarRespuesta(long usuario, long idRespuesta) {
        return sesiones.enLectura(() -> {
            RespuestaVideo r = sesiones.buscarRespuestaPropia(usuario, idRespuesta).orElseThrow(RespuestaNoEncontradaException::new);
            SesionEntrevista s = sesiones.buscarPropia(usuario, r.idSesion()).orElseThrow(RespuestaNoEncontradaException::new);
            return vista(r, s, true);
        });
    }
    @Override public VistaRespuesta reintentarRespuesta(long usuario, long idRespuesta) {
        procesamiento.conRespuestaBloqueada(idRespuesta, datos -> {
            if (datos.sesion().idUsuario()!=usuario) throw new RespuestaNoEncontradaException();
            var c=new ContextoProcesamiento(datos.respuesta());
            procesamiento.actualizar(c.reintentar(datos.sesion(),LocalDateTime.now(clock).withNano(0)));
            return Boolean.TRUE;
        }).orElseThrow(RespuestaNoEncontradaException::new);
        return consultarRespuesta(usuario,idRespuesta);
    }
    @Override public ReproduccionVideo reproducirRespuesta(long usuario,long idRespuesta) {
        var r=sesiones.buscarRespuestaPropia(usuario,idRespuesta).orElseThrow(RespuestaNoEncontradaException::new);
        try {
            if(!almacenamiento.existePrivado(r.publicIdCloud()))throw new VideoNoDisponibleException();
        }catch(FalloEtapaException ex){throw new AlmacenamientoNoDisponibleException();}
        var inicio=clock.instant().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        return new ReproduccionVideo(idRespuesta,firma.firmar(r.publicIdCloud(),inicio),inicio.plusSeconds(300).atOffset(ZoneOffset.UTC));
    }
    private VistaRespuesta vista(RespuestaVideo r, SesionEntrevista s, boolean existente) {
        Puesto puesto = catalogo.buscarPuesto(s.idPuesto()).orElseThrow(PuestoNoEncontradoException::new);
        Pregunta pregunta = catalogo.buscarPreguntasActivas(s.idPuesto()).stream().filter(p -> p.idPregunta().equals(r.idPregunta()))
                .findFirst().orElseThrow(PreguntaNoEncontradaException::new);
        return new VistaRespuesta(r, s, puesto, pregunta, existente,
                new ContextoProcesamiento(r).puedeReintentar(s,LocalDateTime.now(clock).withNano(0)),
                procesamiento.buscarMetrica(r.idRespuesta()).orElse(null),procesamiento.buscarTranscripcion(r.idRespuesta()).orElse(null),
                procesamiento.buscarEvaluacion(r.idRespuesta()).orElse(null));
    }
}
