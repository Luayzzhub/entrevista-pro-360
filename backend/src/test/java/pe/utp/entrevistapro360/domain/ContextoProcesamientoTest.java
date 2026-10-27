package pe.utp.entrevistapro360.domain;
import java.math.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class ContextoProcesamientoTest {
    final LocalDateTime now=LocalDateTime.of(2026,10,25,12,0);
    RespuestaVideo nueva() { return RespuestaVideo.confirmarSubida(1L,1,"video.webm",new BigDecimal("2"),"webm",100L,now,"stt","llm"); }
    SesionEntrevista sesion() { return new SesionEntrevista(1L,1L,1,SesionEntrevista.EstadoSesion.EN_PROCESO,null,now,null); }
    ContextoProcesamiento c(RespuestaVideo r) { return new ContextoProcesamiento(r); }
    @Test void claimCuentaAntesDelTrabajoLeaseEstrictoVersionEInmutabilidad() {
        var r=nueva(); var claimed=c(r).reclamar(now);
        assertThat(c(claimed).version()).isEqualTo(1); assertThat(c(claimed).intentos()).isEqualTo(1);
        assertThat(c(r).intentos()).isZero();
        assertThat(c(claimed).elegible(sesion(),now.plusSeconds(90))).isFalse();
        assertThat(c(claimed).elegible(sesion(),now.plusSeconds(91))).isTrue();
        assertThat(c(c(claimed).renovar(now.plusSeconds(20))).leaseLibre(now.plusSeconds(109))).isFalse();
    }
    @Test void backoffMaximoRetryAfterYCiclo1AgotadoPermiteUnSoloManual() {
        var r=nueva();
        r=c(c(r).reclamar(now)).fallar("TEMP",true,now,12,5,15);
        assertThat(r.procesamientoContexto().get("proximo_intento_utc")).isEqualTo(ContextoProcesamiento.utc(now.plusSeconds(12)));
        assertThat(c(r).elegible(sesion(),now.plusSeconds(11))).isFalse();
        assertThat(c(r).elegible(sesion(),now.plusSeconds(12))).isTrue();
        r=c(c(r).reclamar(now.plusSeconds(12))).fallar("TEMP",true,now,0,5,15);
        assertThat(r.procesamientoContexto().get("proximo_intento_utc")).isEqualTo(ContextoProcesamiento.utc(now.plusSeconds(15)));
        r=c(c(r).reclamar(now.plusSeconds(16))).fallar("TEMP",true,now,0,5,15);
        assertThat(c(r).puedeReintentar(sesion(),now)).isTrue(); assertThat(c(r).definitivo()).isFalse();
        var manual=c(r).reintentar(sesion(),now);
        assertThat(c(manual).ciclo()).isEqualTo(2); assertThat(c(manual).intentos()).isZero();
        assertThat(manual.estadoProcesamiento()).isEqualTo(RespuestaVideo.EstadoProcesamiento.SUBIDO);
        final var primerManual=manual;
        assertThatThrownBy(()->c(primerManual).reintentar(sesion(),now)).isInstanceOf(ReintentoNoDisponibleException.class);
        for(int i=0;i<3;i++) manual=c(c(manual).reclamar(now)).fallar("TEMP",true,now,0,5,15);
        assertThat(c(manual).definitivo()).isTrue(); assertThat(c(manual).recuperable()).isFalse();
    }
    @Test void caidaTercerIntentoNoAdmiteCuartoYFinalizadoNoEsElegible() {
        var r=nueva(); for(int i=0;i<3;i++) r=c(r).reclamar(now);
        var agotada=c(r).agotarLease(now.plusSeconds(91),5,15);
        assertThat(c(agotada).intentos()).isEqualTo(3); assertThat(c(agotada).version()).isEqualTo(4);
        final var tercera=r; assertThatThrownBy(()->c(tercera).reclamar(now)).isInstanceOf(IllegalStateException.class);
        var terminada=c(c(c(nueva()).audioExtraido("audio.wav")).transcrito()).metricasCompletas();
        assertThat(c(terminada).etapa()).isEqualTo(ContextoProcesamiento.Etapa.EVALUAR);
        assertThat(c(terminada).elegible(sesion(),now)).isTrue();
        assertThat(c(c(terminada).evaluado()).elegible(sesion(),now)).isFalse();
        assertThat(c(nueva()).elegible(sesion().finalizarAnticipadamente(now),now)).isFalse();
    }
    @Test void errorDefinitivoNoProgramaIntentosNiPermiteManual() {
        var r=c(c(nueva()).reclamar(now)).fallar("TRANSCRIPCION_VACIA",false,now,100,5,15);
        assertThat(c(r).definitivo()).isTrue(); assertThat(c(r).puedeReintentar(sesion(),now)).isFalse();
        assertThat(r.procesamientoContexto()).containsEntry("proximo_intento_utc",null).containsEntry("bloqueo_hasta_utc",null);
    }
}

