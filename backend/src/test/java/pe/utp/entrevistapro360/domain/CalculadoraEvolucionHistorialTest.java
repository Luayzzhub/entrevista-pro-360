package pe.utp.entrevistapro360.domain;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import pe.utp.entrevistapro360.domain.SesionEntrevista.EstadoSesion;
import pe.utp.entrevistapro360.domain.RespuestaVideo.EstadoProcesamiento;

class CalculadoraEvolucionHistorialTest {
    private static final LocalDateTime INICIO = LocalDateTime.parse("2026-10-05T10:00:00");
    private RegistroHistorial registro(long id, int puesto, String score, LocalDateTime fin) {
        var s = new SesionEntrevista(id, 1L, puesto, EstadoSesion.COMPLETADA, new BigDecimal(score), INICIO, fin);
        List<RespuestaVideo> rs = new ArrayList<>(); Map<Long, EvaluacionSTAR> es = new HashMap<>();
        for (int i=1;i<=3;i++) {
            long r = id*10+i;
            rs.add(new RespuestaVideo(r,id,i,"clave-test",new BigDecimal("10"),"webm",100L,EstadoProcesamiento.EVALUADO,INICIO,Map.of("test",true)));
            es.put(r, new EvaluacionSTAR(r,r,new BigDecimal(score),null,null,null,null,false,null,"COM-1.0","f",Map.of(),"r",fin));
        }
        return new RegistroHistorial(s,rs,es);
    }
    private RegistroHistorial registro(long id, String score) { return registro(id,1,score,INICIO.plusDays(id)); }
    private ComparacionHistorial calcular(List<RegistroHistorial> rs) { return CalculadoraEvolucionHistorial.calcular(1,"COM-1.0",rs); }
    @Test void casoCanonicoPM17() {
        var c = calcular(List.of(registro(4,"83.33"),registro(2,"75.00"),registro(1,"87.50"),registro(3,"79.17")));
        assertThat(c.totalSesionesComparadas()).isEqualTo(4);
        assertThat(c.promedioConjunto()).isEqualTo(new BigDecimal("81.25"));
        assertThat(c.mejorPuntaje()).isEqualTo(new BigDecimal("87.50"));
        assertThat(c.cambioVsPrimera()).isEqualTo(new BigDecimal("-4.17"));
        assertThat(c.cambioVsAnterior()).isEqualTo(new BigDecimal("4.16"));
    }
    @Test void ceroSesionesNoInventaCeros() {
        var c=calcular(List.of()); assertThat(c.habilitada()).isTrue(); assertThat(c.totalSesionesComparadas()).isZero();
        assertThat(c.promedioConjunto()).isNull(); assertThat(c.mejorPuntaje()).isNull();
        assertThat(c.cambioVsPrimera()).isNull();assertThat(c.cambioVsAnterior()).isNull();assertThat(c.serie()).isEmpty();
    }
    @Test void unaSesionNoInventaVariacion() {
        var c=calcular(List.of(registro(1,"0.00")));assertThat(c.promedioConjunto()).isEqualTo(new BigDecimal("0.00"));
        assertThat(c.mejorPuntaje()).isEqualTo(new BigDecimal("0.00"));assertThat(c.cambioVsPrimera()).isNull();assertThat(c.cambioVsAnterior()).isNull();
    }
    @Test void empatePorFechaFinUsaIdAscendente() {
        var c=calcular(List.of(registro(3,1,"80",INICIO.plusDays(1)),registro(2,1,"20",INICIO.plusDays(1)),registro(1,1,"50",INICIO.plusDays(2))));
        assertThat(c.serie()).extracting(SesionEntrevista::idSesion).containsExactly(2L,3L,1L);
        assertThat(c.cambioVsPrimera()).isEqualByComparingTo("30");assertThat(c.cambioVsAnterior()).isEqualByComparingTo("-30");
    }
    @Test void estadosIncompletosYOtroPuestoQuedanFuera() {
        List<RegistroHistorial> rs=new ArrayList<>();rs.add(registro(1,2,"90",INICIO.plusDays(1)));
        for(var estado:List.of(EstadoSesion.INICIADA,EstadoSesion.EN_PROCESO,EstadoSesion.CANCELADA)) {
            var ref=registro(2,"75");var s=new SesionEntrevista(2L,1L,1,estado,null,INICIO,estado==EstadoSesion.CANCELADA?INICIO:null);
            rs.add(new RegistroHistorial(s,ref.respuestas(),ref.evaluaciones()));
        }
        assertThat(calcular(rs).serie()).isEmpty();
    }
    @Test void faltanEvaluacionesORubricaDistintaNoEsComparable() {
        var r=registro(1,"75");var es=new HashMap<>(r.evaluaciones());es.remove(11L);
        assertThat(new RegistroHistorial(r.sesion(),r.respuestas(),es).esComparable()).isFalse();
        var e=r.evaluaciones().get(11L);es.put(11L,new EvaluacionSTAR(11L,11L,e.puntajeGeneral(),null,null,null,null,false,null,"COM-2.0","f",Map.of(),"r",INICIO));
        assertThat(new RegistroHistorial(r.sesion(),r.respuestas(),es).esComparable()).isFalse();
    }
    @Test void identidadesRepetidasORespuestaAjenaNoEsComparable() {
        var r=registro(1,"75");var rs=new ArrayList<>(r.respuestas());rs.set(2,rs.get(0));
        assertThat(new RegistroHistorial(r.sesion(),rs,r.evaluaciones()).esComparable()).isFalse();
        var q=r.respuestas().get(0);rs=new ArrayList<>(r.respuestas());
        rs.set(0,new RespuestaVideo(q.idRespuesta(),2L,q.idPregunta(),q.publicIdCloud(),q.duracionSegundos(),q.formato(),q.tamanoBytes(),q.estadoProcesamiento(),q.fechaGrabacion(),q.procesamientoContexto()));
        assertThat(new RegistroHistorial(r.sesion(),rs,r.evaluaciones()).esComparable()).isFalse();
    }
    @Test void respuestaNoEvaluadaNoEsComparable() {
        var r=registro(1,"75");var q=r.respuestas().get(0);var rs=new ArrayList<>(r.respuestas());
        rs.set(0,new RespuestaVideo(q.idRespuesta(),1L,q.idPregunta(),q.publicIdCloud(),q.duracionSegundos(),q.formato(),q.tamanoBytes(),EstadoProcesamiento.TRANSCRITO,q.fechaGrabacion(),q.procesamientoContexto()));
        assertThat(new RegistroHistorial(r.sesion(),rs,r.evaluaciones()).esComparable()).isFalse();
    }
    @Test void comparacionRequierePuestoYVersionConcretos() {
        for(var c:List.of(CalculadoraEvolucionHistorial.calcular(null,"COM-1.0",List.of(registro(1,"90"))),
                CalculadoraEvolucionHistorial.calcular(1,null,List.of(registro(1,"90"))))) {
            assertThat(c.habilitada()).isFalse();assertThat(c.totalSesionesComparadas()).isZero();assertThat(c.serie()).isEmpty();assertThat(c.promedioConjunto()).isNull();
        }
    }
    @Test void mediaHalfUpAEscalaDos() { assertThat(calcular(List.of(registro(1,"0"),registro(2,"0.01"))).promedioConjunto()).isEqualTo(new BigDecimal("0.01")); }
    @Test void filtroConvierteFechaLimaConFinExclusivo() {
        var f=new FiltroHistorial(null,LocalDate.parse("2026-10-16"),LocalDate.parse("2026-10-16"),null,null,0,10);
        for(var hora:List.of("2026-10-16T04:59:59","2026-10-17T05:00:00")) assertThat(f.incluye(iniciada(hora))).isFalse();
        for(var hora:List.of("2026-10-16T05:00:00","2026-10-17T04:30:00","2026-10-17T04:59:59")) assertThat(f.incluye(iniciada(hora))).isTrue();
    }
    private RegistroHistorial iniciada(String inicio) {return new RegistroHistorial(new SesionEntrevista(1L,1L,1,EstadoSesion.INICIADA,null,LocalDateTime.parse(inicio),null),List.of(),Map.of());}
    @Test void filtroValidaRangosYParametros() {
        assertThatThrownBy(()->new FiltroHistorial(null,LocalDate.parse("2026-10-17"),LocalDate.parse("2026-10-16"),null,null,0,10)).isInstanceOf(FiltroHistorialInvalidoException.class).hasMessage("La fecha inicial no puede ser posterior a la fecha final.");
        for(var f:List.of(new int[]{-1,10},new int[]{0,0},new int[]{0,51})) assertThatThrownBy(()->new FiltroHistorial(null,null,null,null,null,f[0],f[1])).isInstanceOf(FiltroHistorialInvalidoException.class);
    }
}
