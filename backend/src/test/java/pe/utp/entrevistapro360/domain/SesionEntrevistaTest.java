package pe.utp.entrevistapro360.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static pe.utp.entrevistapro360.domain.SesionEntrevista.EstadoSesion.*;
import static pe.utp.entrevistapro360.domain.RespuestaVideo.EstadoProcesamiento.*;

class SesionEntrevistaTest {

    private ResultadoRespuestaSesion procesada(int orden,int... puntos) {
        var r=respuesta(orden,EVALUADO);
        var t=new Transcripcion((long)orden,r.idRespuesta(),"Esta respuesta explica una evidencia",null,"es",inicio);
        var m=CalculadorMetricas.calcular(r.idRespuesta(),t.textoTranscrito(),r.duracionSegundos(),inicio);
        var salida=pe.utp.entrevistapro360.support.EvaluacionFixtures.salida(preguntas.get(orden-1).aplicaStar());
        @SuppressWarnings("unchecked") var criterios=(Map<String,Object>)salida.get("criterios");
        for(int i=0;i<4;i++) {
            @SuppressWarnings("unchecked") var c=(Map<String,Object>)criterios.get(ValidadorEvaluacion.CRITERIOS.get(i));
            c.put("puntaje",puntos[i]);
        }
        return new ResultadoRespuestaSesion(r,t,m,ValidadorEvaluacion.validar(r.idRespuesta(),salida,preguntas.get(orden-1).aplicaStar(),inicio));
    }
    private List<ResultadoRespuestaSesion> tresResultados() {
        return List.of(procesada(1,75,100,50,75),procesada(2,75,100,75,75),procesada(3,75,100,75,100));
    }
    @Test void calcularPromedioGlobalCompletaConTresValidasYPromedioExacto() {
        var cierre=sesion(EN_PROCESO).calcularPromedioGlobal(preguntas,tresResultados(),inicio.plusMinutes(5).withNano(987654321));
        assertThat(cierre.estadoSesion()).isEqualTo(COMPLETADA);
        assertThat(cierre.puntajePromedio()).isEqualTo(new BigDecimal("81.25"));
        assertThat(cierre.fechaFin()).isEqualTo(inicio.plusMinutes(5));
        assertThat(cierre.determinarPreguntaHabilitada(preguntas,tresResultados().stream().map(ResultadoRespuestaSesion::respuesta).toList())).isEmpty();
    }
    @Test void calcularPromedioGlobalHalfUpNoConfundeCerosRealesConAusencias() {
        var redondeo=List.of(procesada(1,0,0,0,0),procesada(2,0,0,0,0),procesada(3,0,0,0,25));
        assertThat(sesion(EN_PROCESO).calcularPromedioGlobal(preguntas,redondeo,inicio).puntajePromedio()).isEqualTo(new BigDecimal("2.08"));
        var ceros=List.of(procesada(1,0,0,0,0),procesada(2,0,0,0,0),procesada(3,0,0,0,0));
        assertThat(sesion(EN_PROCESO).calcularPromedioGlobal(preguntas,ceros,inicio).puntajePromedio()).isEqualTo(new BigDecimal("0.00"));
    }
    @Test void calcularPromedioGlobalNoRellenaConCerosNiCierraConMenosDeTres() {
        var s=sesion(EN_PROCESO);
        for(int n=0;n<3;n++) {
            var sinCerrar=s.calcularPromedioGlobal(preguntas,tresResultados().subList(0,n),inicio);
            assertThat(sinCerrar).isSameAs(s);assertThat(sinCerrar.puntajePromedio()).isNull();assertThat(sinCerrar.fechaFin()).isNull();
        }
    }
    @Test void calcularPromedioGlobalRechazaPreguntaRespuestaRepetidaYDatosDeOtraSesion() {
        var s=sesion(EN_PROCESO);var datos=tresResultados();
        assertThat(s.calcularPromedioGlobal(preguntas,List.of(datos.get(0),datos.get(0),datos.get(2)),inicio)).isSameAs(s);
        assertThat(s.calcularPromedioGlobal(List.of(preguntas.get(0),preguntas.get(0),preguntas.get(2)),datos,inicio)).isSameAs(s);
        var r=datos.get(2).respuesta();
        var ajena=new RespuestaVideo(r.idRespuesta(),100L,r.idPregunta(),r.publicIdCloud(),r.duracionSegundos(),r.formato(),r.tamanoBytes(),r.estadoProcesamiento(),inicio,r.procesamientoContexto());
        var otra=new ResultadoRespuestaSesion(ajena,datos.get(2).transcripcion(),datos.get(2).metrica(),datos.get(2).evaluacion());
        assertThat(s.calcularPromedioGlobal(preguntas,List.of(datos.get(0),datos.get(1),otra),inicio)).isSameAs(s);
    }
    @Test void calcularPromedioGlobalNoCierraSinBloquesPersistidosOConVersionInvalida() {
        var s=sesion(EN_PROCESO);var datos=tresResultados();var r=datos.get(2);
        var metrica=r.metrica();
        var vieja=new MetricaComunicacion(metrica.idMetrica(),metrica.idRespuesta(),metrica.conteoPalabras(),metrica.palabrasPorMinuto(),metrica.conteoMuletillas(),metrica.detalleMuletillas(),"MET-OTRA",inicio);
        var vacia=new Transcripcion(3L,r.respuesta().idRespuesta(),"  ",null,"es",inicio);
        var e=r.evaluacion();
        var invalida=new EvaluacionSTAR(e.idEvaluacion(),e.idRespuesta(),new BigDecimal("99.00"),e.puntajeRelevancia(),e.puntajeClaridad(),e.puntajeEstructura(),e.puntajeEvidencias(),e.aplicaStar(),e.detalleStar(),e.versionRubrica(),e.fortalezaPrincipal(),e.areasMejora(),e.recomendaciones(),e.fechaEvaluacion());
        var casos=List.of(new ResultadoRespuestaSesion(r.respuesta(),null,r.metrica(),e),new ResultadoRespuestaSesion(r.respuesta(),vacia,r.metrica(),e),
                new ResultadoRespuestaSesion(r.respuesta(),r.transcripcion(),null,e),new ResultadoRespuestaSesion(r.respuesta(),r.transcripcion(),vieja,e),
                new ResultadoRespuestaSesion(r.respuesta(),r.transcripcion(),r.metrica(),null),new ResultadoRespuestaSesion(r.respuesta(),r.transcripcion(),r.metrica(),invalida));
        for(var caso:casos)assertThat(s.calcularPromedioGlobal(preguntas,List.of(datos.get(0),datos.get(1),caso),inicio)).isSameAs(s);
    }
    @Test void calcularPromedioGlobalRechazaRespuestaSinEvaluarYBancoInactivo() {
        var s=sesion(EN_PROCESO);var datos=tresResultados();var d=datos.get(2);
        var pendiente=new ResultadoRespuestaSesion(respuesta(3,TRANSCRITO),d.transcripcion(),d.metrica(),d.evaluacion());
        assertThat(s.calcularPromedioGlobal(preguntas,List.of(datos.get(0),datos.get(1),pendiente),inicio)).isSameAs(s);
        var p=preguntas.get(2);
        var inactiva=new Pregunta(p.idPregunta(),p.idPuesto(),p.enunciado(),p.competenciaEvaluada(),p.orden(),p.tiempoLimiteSeg(),false,p.aplicaStar());
        assertThat(s.calcularPromedioGlobal(List.of(preguntas.get(0),preguntas.get(1),inactiva),datos,inicio)).isSameAs(s);
    }
    @Test void calcularPromedioGlobalRespetaCanceladaCompletadaEIniciada() {
        for(var estado:List.of(CANCELADA,COMPLETADA,INICIADA)) {
            var s=sesion(estado);assertThat(s.calcularPromedioGlobal(preguntas,tresResultados(),inicio.plusHours(1))).isSameAs(s);
        }
        var cancelada=sesion(CANCELADA);
        assertThat(cancelada.calcularPromedioGlobal(preguntas,tresResultados(),inicio).puntajePromedio()).isNull();
        var completada=sesion(EN_PROCESO).calcularPromedioGlobal(preguntas,tresResultados(),inicio);
        assertThatThrownBy(()->completada.finalizarAnticipadamente(inicio)).isInstanceOf(TransicionSesionInvalidaException.class);
    }
    @Test void calcularPromedioGlobalNoRetrocedeFechaInicio() {
        var cierre=sesion(EN_PROCESO).calcularPromedioGlobal(preguntas,tresResultados(),inicio.minusDays(1));
        assertThat(cierre.fechaFin()).isEqualTo(inicio);
    }
    private final LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 15, 0);
    private final Puesto puesto = new Puesto(7, "Soporte", "TI", "Descripción", "Inicial", true);
    private final List<Pregunta> preguntas = List.of(pregunta(1), pregunta(2), pregunta(3));
    private Pregunta pregunta(int orden) { return new Pregunta(10 + orden, 7, "Enunciado " + orden, "Competencia", orden, 60, true, orden == 2); }
    private SesionEntrevista sesion(SesionEntrevista.EstadoSesion estado) {
        return new SesionEntrevista(99L, 5L, 7, estado, estado == COMPLETADA ? new BigDecimal("80") : null,
                inicio, estado == COMPLETADA || estado == CANCELADA ? inicio.plusMinutes(1) : null);
    }
    private RespuestaVideo respuesta(int orden, RespuestaVideo.EstadoProcesamiento estado) {
        return new RespuestaVideo((long) orden, 99L, 10 + orden, "fixture", new BigDecimal("30.000"), "webm", 1000L, estado, inicio, Map.of("origen", "test"));
    }
    @Test void iniciarValidaCatalogoYTruncaUtcASegundos() {
        var s = SesionEntrevista.iniciar(5L, puesto, preguntas, inicio.withNano(123456));
        assertThat(s.idSesion()).isNull(); assertThat(s.estadoSesion()).isEqualTo(INICIADA);
        assertThat(s.fechaInicio()).isEqualTo(inicio); assertThat(s.fechaFin()).isNull(); assertThat(s.puntajePromedio()).isNull();
        assertThat(s.admiteRespuestas()).isTrue();
    }
    @Test void iniciarRechazaPuestoNuloInactivoYCatalogoIncompletoODuplicado() {
        assertThatThrownBy(() -> SesionEntrevista.iniciar(5L, null, preguntas, inicio)).isInstanceOf(CatalogoInvalidoException.class);
        var inactivo = new Puesto(7, "Soporte", "TI", null, "Inicial", false);
        assertThatThrownBy(() -> SesionEntrevista.iniciar(5L, inactivo, preguntas, inicio)).isInstanceOf(CatalogoInvalidoException.class);
        assertThatThrownBy(() -> SesionEntrevista.iniciar(5L, puesto, preguntas.subList(0, 2), inicio)).isInstanceOf(CatalogoInvalidoException.class);
        assertThatThrownBy(() -> SesionEntrevista.iniciar(5L, puesto, List.of(pregunta(1), pregunta(1), pregunta(3)), inicio)).isInstanceOf(CatalogoInvalidoException.class);
    }
    @Test void rechazaIdentidadEstadoOFechaInicialInvalidos() {
        for (Long id : Arrays.asList(null, 0L, -1L)) assertThatThrownBy(() -> new SesionEntrevista(null, id, 7, INICIADA, null, inicio, null)).isInstanceOf(IllegalArgumentException.class);
        for (Integer id : Arrays.asList(null, 0, -1)) assertThatThrownBy(() -> new SesionEntrevista(null, 5L, id, INICIADA, null, inicio, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SesionEntrevista(null, 5L, 7, null, null, inicio, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SesionEntrevista(null, 5L, 7, INICIADA, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void sesionNoTerminalNoAdmitePromedioNiFechaFinal() {
        for (var estado : List.of(INICIADA, EN_PROCESO)) {
            assertThatThrownBy(() -> new SesionEntrevista(99L, 5L, 7, estado, BigDecimal.ZERO, inicio, null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new SesionEntrevista(99L, 5L, 7, estado, null, inicio, inicio)).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void terminalRequiereFechaFinalValidaYPromedioSegunEstado() {
        for (var estado : List.of(CANCELADA, COMPLETADA)) {
            BigDecimal promedio = estado == COMPLETADA ? BigDecimal.TEN : null;
            assertThatThrownBy(() -> new SesionEntrevista(99L, 5L, 7, estado, promedio, inicio, null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new SesionEntrevista(99L, 5L, 7, estado, promedio, inicio, inicio.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new SesionEntrevista(99L, 5L, 7, CANCELADA, BigDecimal.ZERO, inicio, inicio)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SesionEntrevista(99L, 5L, 7, COMPLETADA, null, inicio, inicio)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void promedioCompletadoValidaRangoYRedondeaHalfUpAEscalaDos() {
        for (String valor : List.of("-0.001", "100.001")) assertThatThrownBy(() -> new SesionEntrevista(99L, 5L, 7, COMPLETADA, new BigDecimal(valor), inicio, inicio)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new SesionEntrevista(99L, 5L, 7, COMPLETADA, new BigDecimal("89.345"), inicio, inicio).puntajePromedio()).isEqualTo(new BigDecimal("89.35"));
        assertThat(new SesionEntrevista(99L, 5L, 7, COMPLETADA, BigDecimal.ZERO, inicio, inicio).puntajePromedio()).isEqualTo(new BigDecimal("0.00"));
        assertThat(new SesionEntrevista(99L, 5L, 7, COMPLETADA, new BigDecimal("100"), inicio, inicio).puntajePromedio()).isEqualTo(new BigDecimal("100.00"));
    }
    @Test void sinRespuestasHabilitaPrimeraYOrdenaElBanco() {
        assertThat(sesion(INICIADA).determinarPreguntaHabilitada(List.of(pregunta(3), pregunta(1), pregunta(2)), List.of())).contains(pregunta(1));
    }
    @Test void cualquierEtapaNoEvaluadaBloqueaElAvanceIncluidoFallido() {
        for (var estado : List.of(SUBIDO, AUDIO_EXTRAIDO, TRANSCRITO, FALLIDO)) {
            assertThat(sesion(EN_PROCESO).determinarPreguntaHabilitada(preguntas, List.of(respuesta(1, estado)))).isEmpty();
        }
    }
    @Test void evaluadasDesbloqueanSegundaYTercera() {
        assertThat(sesion(EN_PROCESO).determinarPreguntaHabilitada(preguntas, List.of(respuesta(1, EVALUADO)))).contains(pregunta(2));
        assertThat(sesion(EN_PROCESO).determinarPreguntaHabilitada(preguntas, List.of(respuesta(1, EVALUADO), respuesta(2, EVALUADO)))).contains(pregunta(3));
    }
    @Test void tresEvaluadasOTerminalNoHabilitanPreguntas() {
        assertThat(sesion(EN_PROCESO).determinarPreguntaHabilitada(preguntas, List.of(respuesta(1, EVALUADO), respuesta(2, EVALUADO), respuesta(3, EVALUADO)))).isEmpty();
        for (var estado : List.of(CANCELADA, COMPLETADA)) { assertThat(sesion(estado).determinarPreguntaHabilitada(preguntas, List.of())).isEmpty(); assertThat(sesion(estado).admiteRespuestas()).isFalse(); }
    }
    @Test void validaCatalogoTambienAlConsultarAvance() {
        assertThatThrownBy(() -> sesion(INICIADA).determinarPreguntaHabilitada(preguntas.subList(0, 2), List.of())).isInstanceOf(CatalogoInvalidoException.class);
    }
    @Test void cuentaSoloEvaluadasDeEstaSesion() {
        var ajena = new RespuestaVideo(4L, 100L, 13, "fixture", BigDecimal.ONE, "webm", 100L, EVALUADO, inicio, Map.of("origen", "test"));
        assertThat(sesion(EN_PROCESO).contarRespuestasEvaluadas(List.of(respuesta(1, EVALUADO), respuesta(2, SUBIDO), ajena))).isEqualTo(1);
    }
    @Test void cancelaAmbosEstadosNoTerminalesYConservaIdentidad() {
        for (var estado : List.of(INICIADA, EN_PROCESO)) {
            var s = sesion(estado).finalizarAnticipadamente(inicio.plusMinutes(2).withNano(456));
            assertThat(s.estadoSesion()).isEqualTo(CANCELADA); assertThat(s.puntajePromedio()).isNull();
            assertThat(s.fechaFin()).isEqualTo(inicio.plusMinutes(2)); assertThat(s.idSesion()).isEqualTo(99L);
            assertThat(s.fechaInicio()).isEqualTo(inicio);
        }
    }
    @Test void cancelacionIdempotenteNoCambiaFechaYCompletadaNoSeCancela() {
        var cancelada = sesion(CANCELADA);
        assertThat(cancelada.finalizarAnticipadamente(inicio.plusDays(1))).isSameAs(cancelada);
        assertThatThrownBy(() -> sesion(COMPLETADA).finalizarAnticipadamente(inicio.plusDays(1))).isInstanceOf(TransicionSesionInvalidaException.class);
    }
    @Test void relojRetrocedidoNoProduceFechaFinalAnteriorAlInicio() {
        assertThat(sesion(INICIADA).finalizarAnticipadamente(inicio.minusDays(1)).fechaFin()).isEqualTo(inicio);
    }
}
