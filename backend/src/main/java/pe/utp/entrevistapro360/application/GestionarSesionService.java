package pe.utp.entrevistapro360.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import pe.utp.entrevistapro360.application.port.in.GestionarSesion;
import pe.utp.entrevistapro360.application.port.out.*;
import pe.utp.entrevistapro360.domain.*;

public final class GestionarSesionService implements GestionarSesion {
    private final SesionRepository sesiones;
    private final CatalogoRepository catalogo;
    private final Clock clock;
    public GestionarSesionService(SesionRepository sesiones, CatalogoRepository catalogo, Clock clock) {
        this.sesiones = sesiones; this.catalogo = catalogo; this.clock = clock;
    }
    @Override public VistaSesion iniciarSesion(long idUsuario, int idPuesto) {
        return sesiones.conUsuarioBloqueado(idUsuario, () -> {
            sesiones.buscarNoTerminal(idUsuario).ifPresent(s -> { throw new SesionActivaExistenteException(s.idSesion()); });
            Puesto puesto = catalogo.buscarPuestoActivo(idPuesto).orElseThrow(PuestoNoEncontradoException::new);
            List<Pregunta> preguntas = catalogo.buscarPreguntasActivas(idPuesto);
            SesionEntrevista nueva = SesionEntrevista.iniciar(idUsuario, puesto, preguntas, LocalDateTime.now(clock).withNano(0));
            return ensamblar(sesiones.guardarNueva(nueva), puesto, preguntas, List.of());
        });
    }
    @Override public VistaSesion consultarSesion(long idUsuario, long idSesion) {
        return sesiones.enLectura(() -> vista(sesiones.buscarPropia(idUsuario, idSesion).orElseThrow(SesionNoEncontradaException::new)));
    }
    @Override public VistaSesion cancelarSesion(long idUsuario, long idSesion) {
        return sesiones.conSesionBloqueada(idUsuario, idSesion, sesion -> {
            SesionEntrevista cancelada = sesion.finalizarAnticipadamente(LocalDateTime.now(clock).withNano(0));
            return vista(cancelada == sesion ? sesion : sesiones.actualizar(cancelada));
        });
    }
    @Override public ResumenSesionesUsuario consultarResumenUsuario(long idUsuario) {
        return sesiones.enLectura(() -> {
            List<SesionEntrevista> propias = sesiones.listarPropias(idUsuario);
            var respuestas = sesiones.buscarRespuestas(propias.stream().map(SesionEntrevista::idSesion).toList()).stream()
                    .collect(Collectors.groupingBy(RespuestaVideo::idSesion));
            Map<Integer, Puesto> puestos = new HashMap<>();
            Map<Integer, List<Pregunta>> preguntas = new HashMap<>();
            List<ResumenSesionItem> items = propias.stream().map(s -> {
                Puesto puesto = puestos.computeIfAbsent(s.idPuesto(), this::buscarPuesto);
                List<Pregunta> banco = preguntas.computeIfAbsent(s.idPuesto(), catalogo::buscarPreguntasActivas);
                VistaSesion vista = ensamblar(s, puesto, banco, respuestas.getOrDefault(s.idSesion(), List.of()));
                return new ResumenSesionItem(s, puesto, vista.respuestasConfirmadas(), vista.respuestasEvaluadas(),
                        vista.preguntaHabilitada() == null ? null : vista.preguntaHabilitada().orden());
            }).toList();
            var completadas = items.stream().filter(i -> i.sesion().estadoSesion() == SesionEntrevista.EstadoSesion.COMPLETADA).toList();
            var ultima = completadas.stream().max(Comparator.comparing((ResumenSesionItem i) -> i.sesion().fechaFin())
                    .thenComparing(i -> i.sesion().idSesion())).orElse(null);
            return new ResumenSesionesUsuario(items.stream().filter(i -> !i.sesion().esTerminal()).findFirst().orElse(null), ultima, completadas.size(), items);
        });
    }
    private Puesto buscarPuesto(int idPuesto) { return catalogo.buscarPuesto(idPuesto).orElseThrow(PuestoNoEncontradoException::new); }
    private VistaSesion vista(SesionEntrevista sesion) {
        return ensamblar(sesion, buscarPuesto(sesion.idPuesto()), catalogo.buscarPreguntasActivas(sesion.idPuesto()), sesiones.buscarRespuestas(List.of(sesion.idSesion())));
    }
    private VistaSesion ensamblar(SesionEntrevista sesion, Puesto puesto, List<Pregunta> preguntas, List<RespuestaVideo> respuestas) {
        Pregunta habilitada = sesion.determinarPreguntaHabilitada(preguntas, respuestas).orElse(null);
        var porPregunta = respuestas.stream().collect(Collectors.toMap(RespuestaVideo::idPregunta, r -> r));
        var estados = preguntas.stream().sorted(Comparator.comparingInt(Pregunta::orden))
                .map(p -> new EstadoPreguntaSesion(p, porPregunta.get(p.idPregunta()), habilitada != null && habilitada.idPregunta().equals(p.idPregunta()))).toList();
        return new VistaSesion(sesion, puesto, estados, habilitada, respuestas.size(), sesion.contarRespuestasEvaluadas(respuestas));
    }
}
