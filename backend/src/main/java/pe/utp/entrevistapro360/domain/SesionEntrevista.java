package pe.utp.entrevistapro360.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public record SesionEntrevista(Long idSesion, Long idUsuario, Integer idPuesto,
                              EstadoSesion estadoSesion, BigDecimal puntajePromedio,
                              LocalDateTime fechaInicio, LocalDateTime fechaFin) {
    public SesionEntrevista {
        if ((idSesion != null && idSesion < 1) || idUsuario == null || idUsuario < 1 || idPuesto == null || idPuesto < 1
                || estadoSesion == null || fechaInicio == null) throw new IllegalArgumentException("Identidad y fecha de sesión inválidas.");
        boolean terminal = estadoSesion == EstadoSesion.COMPLETADA || estadoSesion == EstadoSesion.CANCELADA;
        if (!terminal && (puntajePromedio != null || fechaFin != null)) throw new IllegalArgumentException("Una sesión en curso no tiene promedio ni fecha final.");
        if (terminal && (fechaFin == null || fechaFin.isBefore(fechaInicio))) throw new IllegalArgumentException("La fecha final debe ser posterior o igual al inicio.");
        if (estadoSesion == EstadoSesion.CANCELADA && puntajePromedio != null) throw new IllegalArgumentException("Una sesión cancelada no tiene promedio.");
        if (estadoSesion == EstadoSesion.COMPLETADA) {
            if (puntajePromedio == null || puntajePromedio.compareTo(BigDecimal.ZERO) < 0 || puntajePromedio.compareTo(new BigDecimal("100")) > 0)
                throw new IllegalArgumentException("Una sesión completada requiere promedio entre 0 y 100.");
            puntajePromedio = puntajePromedio.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public static SesionEntrevista iniciar(Long idUsuario, Puesto puesto, List<Pregunta> preguntasActivas, LocalDateTime ahoraUtc) {
        if (puesto == null) throw new CatalogoInvalidoException();
        puesto.validarPreguntasActivas(preguntasActivas);
        return new SesionEntrevista(null, idUsuario, puesto.idPuesto(), EstadoSesion.INICIADA, null,
                Objects.requireNonNull(ahoraUtc).withNano(0), null);
    }
    public boolean esTerminal() { return estadoSesion == EstadoSesion.COMPLETADA || estadoSesion == EstadoSesion.CANCELADA; }
    public boolean admiteRespuestas() { return !esTerminal(); }

    public SesionEntrevista finalizarAnticipadamente(LocalDateTime ahoraUtc) {
        if (estadoSesion == EstadoSesion.CANCELADA) return this;
        if (estadoSesion == EstadoSesion.COMPLETADA) throw new TransicionSesionInvalidaException();
        LocalDateTime fin = Objects.requireNonNull(ahoraUtc).withNano(0);
        if (fin.isBefore(fechaInicio)) fin = fechaInicio;
        return new SesionEntrevista(idSesion, idUsuario, idPuesto, EstadoSesion.CANCELADA, null, fechaInicio, fin);
    }

    public Optional<Pregunta> determinarPreguntaHabilitada(List<Pregunta> preguntasPuesto, List<RespuestaVideo> respuestasSesion) {
        if (preguntasPuesto == null || preguntasPuesto.size() != 3
                || preguntasPuesto.stream().anyMatch(p -> p == null || !p.activo() || !idPuesto.equals(p.idPuesto()))) throw new CatalogoInvalidoException();
        List<Pregunta> ordenadas = preguntasPuesto.stream().sorted(Comparator.comparingInt(Pregunta::orden)).toList();
        if (!ordenadas.stream().map(Pregunta::orden).toList().equals(List.of(1, 2, 3))) throw new CatalogoInvalidoException();
        if (esTerminal()) return Optional.empty();
        for (Pregunta pregunta : ordenadas) {
            Optional<RespuestaVideo> respuesta = respuestasSesion.stream()
                    .filter(r -> Objects.equals(idSesion, r.idSesion()) && pregunta.idPregunta().equals(r.idPregunta())).findFirst();
            if (respuesta.isEmpty()) return Optional.of(pregunta);
            if (respuesta.get().estadoProcesamiento() != RespuestaVideo.EstadoProcesamiento.EVALUADO) return Optional.empty();
        }
        return Optional.empty();
    }
    public int contarRespuestasEvaluadas(List<RespuestaVideo> respuestasSesion) {
        return (int) respuestasSesion.stream().filter(r -> Objects.equals(idSesion, r.idSesion())
                && r.estadoProcesamiento() == RespuestaVideo.EstadoProcesamiento.EVALUADO).count();
    }

    public enum EstadoSesion { INICIADA, EN_PROCESO, COMPLETADA, CANCELADA }
}
