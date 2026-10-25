package pe.utp.entrevistapro360.domain;
import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class RespuestaVideoTest {
    private final LocalDateTime ahora = LocalDateTime.of(2026, 10, 21, 10, 0, 0, 987654);
    private RespuestaVideo respuesta(Long sesion, Integer pregunta, String clave, String duracion, String formato, Long bytes) {
        return new RespuestaVideo(null, sesion, pregunta, clave, duracion == null ? null : new BigDecimal(duracion), formato, bytes,
                RespuestaVideo.EstadoProcesamiento.SUBIDO, ahora, Map.of("origen", "test"));
    }
    @Test void fabricaConstruyeProcExactoConNulosIntentosYModelos() {
        var r = RespuestaVideo.confirmarSubida(1L, 2, "usuarios/1/video.webm", new BigDecimal("2.1235"), "webm", 200L, ahora, "stt-test", "llm-test");
        var c = r.procesamientoContexto();
        assertThat(c).hasSize(13).containsEntry("version_contexto", "PROC-1.0").containsEntry("etapa_actual", "EXTRAER_AUDIO")
                .containsEntry("ultimo_hito_valido", "SUBIDO").containsEntry("ciclo_actual", 1).containsEntry("reintento_manual_usado", false)
                .containsEntry("version_ejecucion", 0).containsEntry("modelo_stt", "stt-test").containsEntry("modelo_llm", "llm-test");
        for (String campo : List.of("ultimo_error", "proximo_intento_utc", "bloqueo_hasta_utc", "referencia_audio")) assertThat(c).containsEntry(campo, null);
        var intentos = (Map<?, ?>) c.get("intentos_por_etapa"); assertThat(intentos).hasSize(4);
        for (Object etapa : intentos.values()) assertThat((Map<String, Object>) etapa).containsExactlyInAnyOrderEntriesOf(Map.of("ciclo_1", 0, "ciclo_2", 0));
        assertThat(r.estadoProcesamiento()).isEqualTo(RespuestaVideo.EstadoProcesamiento.SUBIDO);
        assertThat(r.duracionSegundos()).isEqualTo(new BigDecimal("2.124")); assertThat(r.fechaGrabacion().getNano()).isZero();
        assertThatThrownBy(() -> c.put("x", 1)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ((Map<String, Object>) intentos.get("EVALUAR")).put("x", 1)).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void identidadesDebenSerPositivas() {
        for (Long id : Arrays.asList(null, 0L, -1L)) assertThatThrownBy(() -> respuesta(id, 1, "clave", "2", "webm", 1L)).isInstanceOf(IllegalArgumentException.class);
        for (Integer id : Arrays.asList(null, 0, -1)) assertThatThrownBy(() -> respuesta(1L, id, "clave", "2", "webm", 1L)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void clavePrivadaRechazaUrlsEspaciosRecorridoYLongitud() {
        for (String clave : Arrays.asList(null, "", " ", "http://x", "HTTPS://x", "a..b", "a b", "a\tb", "a\u00a0b", "x".repeat(151)))
            assertThatThrownBy(() -> respuesta(1L, 1, clave, "2", "webm", 1L)).isInstanceOf(IllegalArgumentException.class);
        assertThat(respuesta(1L, 1, "x".repeat(150), "2", "webm", 1L).publicIdCloud()).hasSize(150);
    }
    @Test void validaDuracionAntesDeRedondearInclusiveUnoYSesenta() {
        for (String d : Arrays.asList(null, "0.9999", "60.0001", "60.0004", "-1"))
            assertThatThrownBy(() -> respuesta(1L, 1, "clave", d, "webm", 1L)).isInstanceOf(IllegalArgumentException.class);
        assertThat(respuesta(1L, 1, "clave", "1", "webm", 1L).duracionSegundos()).isEqualTo(new BigDecimal("1.000"));
        assertThat(respuesta(1L, 1, "clave", "60", "webm", 1L).duracionSegundos()).isEqualTo(new BigDecimal("60.000"));
    }
    @Test void tamanosEnterosPositivosMenoresDeCincuentaMillones() {
        for (Long bytes : Arrays.asList(null, 0L, -1L, 50_000_000L))
            assertThatThrownBy(() -> respuesta(1L, 1, "clave", "2", "webm", bytes)).isInstanceOf(IllegalArgumentException.class);
        assertThat(respuesta(1L, 1, "clave", "2", "mp4", 49_999_999L).tamanoBytes()).isEqualTo(49_999_999L);
    }
    @Test void soloWebmYMp4SonAdmitidos() {
        for (String formato : Arrays.asList(null, "", "avi", "WEBM"))
            assertThatThrownBy(() -> respuesta(1L, 1, "clave", "2", formato, 1L)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void contextoFechaYEstadoSonObligatorios() {
        for (Map<String, Object> c : Arrays.asList(null, Map.<String, Object>of()))
            assertThatThrownBy(() -> new RespuestaVideo(null, 1L, 1, "clave", BigDecimal.ONE, "webm", 1L, RespuestaVideo.EstadoProcesamiento.SUBIDO, ahora, c)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RespuestaVideo(null, 1L, 1, "clave", BigDecimal.ONE, "webm", 1L, null, ahora, Map.of("x", 1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RespuestaVideo(null, 1L, 1, "clave", BigDecimal.ONE, "webm", 1L, RespuestaVideo.EstadoProcesamiento.SUBIDO, null, Map.of("x", 1))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void transicionDeSesionPreservaIdentidadEsIdempotenteYRechazaTerminales() {
        var s = new SesionEntrevista(1L, 2L, 1, SesionEntrevista.EstadoSesion.INICIADA, null, ahora, null);
        var proceso = s.registrarRespuestaConfirmada(); assertThat(proceso.estadoSesion()).isEqualTo(SesionEntrevista.EstadoSesion.EN_PROCESO);
        assertThat(proceso.idSesion()).isEqualTo(s.idSesion()); assertThat(proceso.fechaInicio()).isEqualTo(s.fechaInicio());
        assertThat(proceso.registrarRespuestaConfirmada()).isSameAs(proceso);
        assertThatThrownBy(() -> s.finalizarAnticipadamente(ahora).registrarRespuestaConfirmada()).isInstanceOf(TransicionSesionInvalidaException.class);
        assertThatThrownBy(() -> new SesionEntrevista(1L, 2L, 1, SesionEntrevista.EstadoSesion.COMPLETADA, BigDecimal.TEN, ahora, ahora).registrarRespuestaConfirmada()).isInstanceOf(TransicionSesionInvalidaException.class);
    }
}
