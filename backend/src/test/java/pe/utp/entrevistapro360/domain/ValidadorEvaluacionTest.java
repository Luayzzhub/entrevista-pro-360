package pe.utp.entrevistapro360.domain;
import java.util.*;
import java.math.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import pe.utp.entrevistapro360.support.EvaluacionFixtures;
import static org.assertj.core.api.Assertions.*;
class ValidadorEvaluacionTest {
    final LocalDateTime now=LocalDateTime.of(2026,10,25,20,0);
    EvaluacionSTAR validar(Map<String,Object> s,boolean aplica){return ValidadorEvaluacion.validar(1L,s,aplica,now);}
    @SuppressWarnings("unchecked") Map<String,Object> map(Object v){return (Map<String,Object>)v;}
    @Test void promedioBackendEscalaExactaYMapaPersistidoSinDatosExtras(){
        var e=validar(EvaluacionFixtures.salida(false),false);
        assertThat(e.puntajeGeneral()).isEqualTo(new BigDecimal("75.00"));assertThat(e.puntajeRelevancia()).isEqualTo(new BigDecimal("75.00"));
        assertThat(e.areasMejora()).containsOnlyKeys("version_contrato","justificaciones","sintesis");
        assertThat(map(e.areasMejora().get("justificaciones"))).containsExactlyInAnyOrderEntriesOf(Map.of("relevancia","Justificación de relevancia","claridad","Justificación de claridad","estructura","Justificación de estructura","evidencias","Justificación de evidencias"));
        assertThat(e.areasMejora().get("sintesis")).isEqualTo("Síntesis de mejora");assertThat(e.detalleStar()).isNull();
        assertThat(ValidadorEvaluacion.validarGuardada(e,false)).isSameAs(e);
    }
    @Test void cerosYCuartosSinRedondeoIntermedio(){
        var s=EvaluacionFixtures.salida(false);
        for(Object c:map(s.get("criterios")).values())map(c).put("puntaje",0);
        assertThat(validar(s,false).puntajeGeneral()).isEqualTo(new BigDecimal("0.00"));
        map(map(s.get("criterios")).get("relevancia")).put("puntaje",25);
        assertThat(validar(s,false).puntajeGeneral()).isEqualTo(new BigDecimal("6.25"));
    }
    @Test void rechazaEnterosNoDiscretosDecimalesTextoYNull(){
        for(Object n:Arrays.asList(60,-25,101,25.0,new BigDecimal("25.0"),"75",null)){
            var s=EvaluacionFixtures.salida(false);map(map(s.get("criterios")).get("claridad")).put("puntaje",n);
            assertThatThrownBy(()->validar(s,false)).isInstanceOf(SalidaEvaluacionInvalidaException.class);
        }
    }
    @Test void cuatroCriteriosExactosYVersionesCerradas(){
        for(String campo:List.of("version_contrato","version_rubrica","criterios","aplica_star")){
            var s=EvaluacionFixtures.salida(false);s.remove(campo);assertThatThrownBy(()->validar(s,false)).isInstanceOf(SalidaEvaluacionInvalidaException.class);
        }
        var s=EvaluacionFixtures.salida(false);s.put("puntaje_general",100);
        assertThatThrownBy(()->validar(s,false)).isInstanceOf(SalidaEvaluacionInvalidaException.class);
        for(String campo:List.of("version_contrato","version_rubrica")){
            var v=EvaluacionFixtures.salida(false);v.put(campo,"OTRA");assertThatThrownBy(()->validar(v,false)).isInstanceOf(SalidaEvaluacionInvalidaException.class);
        }
        var falta=EvaluacionFixtures.salida(false);map(falta.get("criterios")).remove("evidencias");
        assertThatThrownBy(()->validar(falta,false)).isInstanceOf(SalidaEvaluacionInvalidaException.class);
    }
    @Test void textosLimitesExactosSeRecortanYRechazanVaciosExcesivos(){
        for(String invalido:List.of("","   ","x".repeat(601))){
            var s=EvaluacionFixtures.salida(false);map(map(s.get("criterios")).get("relevancia")).put("justificacion",invalido);
            assertThatThrownBy(()->validar(s,false)).isInstanceOf(SalidaEvaluacionInvalidaException.class);
        }
        for(String campo:List.of("fortaleza_principal","areas_mejora","recomendaciones")){
            for(String v:List.of(" ","x".repeat(1201))){
                var s=EvaluacionFixtures.salida(false);s.put(campo,v);assertThatThrownBy(()->validar(s,false)).isInstanceOf(SalidaEvaluacionInvalidaException.class);
            }
            var ok=EvaluacionFixtures.salida(false);ok.put(campo," "+"x".repeat(1200)+" ");assertThatCode(()->validar(ok,false)).doesNotThrowAnyException();
        }
        var ok=EvaluacionFixtures.salida(false);map(map(ok.get("criterios")).get("relevancia")).put("justificacion"," "+"x".repeat(600)+" ");
        assertThatCode(()->validar(ok,false)).doesNotThrowAnyException();
    }
    @Test void starCoincideConPreguntaEsExactoYCualitativo(){
        var e=validar(EvaluacionFixtures.salida(true),true);assertThat(e.detalleStar()).hasSize(4);
        assertThat(map(e.detalleStar().get("accion"))).containsEntry("observacion","Observación accion");
        assertThat(e.puntajeGeneral()).isEqualTo(new BigDecimal("75.00"));
        assertThatThrownBy(()->validar(EvaluacionFixtures.salida(true),false)).isInstanceOf(SalidaEvaluacionInvalidaException.class);
        var falso=EvaluacionFixtures.salida(false);falso.put("detalle_star",Map.of());assertThatThrownBy(()->validar(falso,false)).isInstanceOf(SalidaEvaluacionInvalidaException.class);
        for(String k:ValidadorEvaluacion.STAR){
            var s=EvaluacionFixtures.salida(true);map(s.get("detalle_star")).remove(k);assertThatThrownBy(()->validar(s,true)).isInstanceOf(SalidaEvaluacionInvalidaException.class);
        }
        for(Object estado:Arrays.asList("OTRO",null)){
            var s=EvaluacionFixtures.salida(true);map(map(s.get("detalle_star")).get("tarea")).put("estado",estado);
            assertThatThrownBy(()->validar(s,true)).isInstanceOf(SalidaEvaluacionInvalidaException.class);
        }
        for(String v:List.of(" ","x".repeat(601))){
            var s=EvaluacionFixtures.salida(true);map(map(s.get("detalle_star")).get("accion")).put("observacion",v);
            assertThatThrownBy(()->validar(s,true)).isInstanceOf(SalidaEvaluacionInvalidaException.class);
        }
    }
    @Test void camposAdicionalesEnObjetosSonInvalidosYMapasPersistidosInmutables(){
        for(boolean star:List.of(false,true)){
            var s=EvaluacionFixtures.salida(star);map(map(s.get("criterios")).get("claridad")).put("extra","x");
            assertThatThrownBy(()->validar(s,star)).isInstanceOf(SalidaEvaluacionInvalidaException.class);
        }
        var e=validar(EvaluacionFixtures.salida(true),true);
        assertThatThrownBy(()->e.areasMejora().put("x",1)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(()->map(e.detalleStar().get("accion")).put("estado","AUSENTE")).isInstanceOf(UnsupportedOperationException.class);
    }
}

