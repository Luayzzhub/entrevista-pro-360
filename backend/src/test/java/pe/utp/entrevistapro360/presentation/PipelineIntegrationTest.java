package pe.utp.entrevistapro360.presentation;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import software.amazon.awssdk.services.s3.S3Client;
import pe.utp.entrevistapro360.support.*;
import pe.utp.entrevistapro360.application.*;
import pe.utp.entrevistapro360.application.port.out.*;
import pe.utp.entrevistapro360.domain.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@Import(PipelineIntegrationTest.Config.class)
class PipelineIntegrationTest {
    static final S3HttpFixture S3=new S3HttpFixture();
    static final SttHttpFixture STT=new SttHttpFixture();
    static final class Reloj extends Clock {
        volatile Instant instante=Instant.parse("2026-10-25T18:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { if(!zone.equals(ZoneOffset.UTC)) throw new IllegalArgumentException(); return this; }
        public Instant instant() { return instante; }
        void avanzar(long segundos) { instante=instante.plusSeconds(segundos); }
    }
    @TestConfiguration static class Config {
        @Bean @Primary S3Client s3Local() { return S3.cliente(); }
        @Bean @Primary Reloj reloj() { return new Reloj(); }
    }
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r) {
        r.add("integraciones.s3-bucket",()->"pruebas");
        r.add("integraciones.openai-base-url",STT::endpoint); r.add("integraciones.openai-api-key",()->"clave-local");
        r.add("integraciones.stt-model",()->"gpt-4o-mini-transcribe-2025-12-15");
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired ProcesarRespuestasService worker; @Autowired ProcesamientoRepository repo;
    @Autowired AlmacenamientoVideoPort almacenamiento; @Autowired ExtractorAudioPort extractor;
    @Autowired TranscripcionSttPort stt; @Autowired Reloj reloj;
    String token; long usuario,sesion,id; int pregunta;
    @BeforeEach void preparar() throws Exception {
        S3.limpiar();STT.limpiar();reloj.instante=Instant.parse("2026-10-25T18:00:00Z");
        String email="pipeline-"+UUID.randomUUID()+"@example.test",password="pipeline-password-2026";
        var registro=mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre_completo","Persona Privada","email",email,"password",password))))
                .andExpect(status().isCreated()).andReturn().getResponse();
        usuario=json.readTree(registro.getContentAsString()).get("id_usuario").asLong();
        var login=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",email,"password",password)))).andExpect(status().isOk()).andReturn().getResponse();
        token=json.readTree(login.getContentAsString()).get("access_token").asText();
        int puesto=jdbc.queryForObject("SELECT MIN(id_puesto) FROM puestos",Integer.class);
        pregunta=jdbc.queryForObject("SELECT id_pregunta FROM preguntas WHERE id_puesto=? AND orden=1",Integer.class,puesto);
        var creada=mvc.perform(post("/api/v1/sesiones").header("Authorization",auth()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("id_puesto",puesto)))).andExpect(status().isCreated()).andReturn().getResponse();
        sesion=json.readTree(creada.getContentAsString()).get("id_sesion").asLong();
        var subida=mvc.perform(multipart(HttpMethod.PUT,"/api/v1/sesiones/{id}/preguntas/{p}/respuesta",sesion,pregunta)
                .file(new MockMultipartFile("video","toma.webm","video/webm",Files.readAllBytes(MediaFixtures.archivo("webm"))))
                .header("Authorization",auth())).andExpect(status().isAccepted()).andReturn().getResponse();
        id=json.readTree(subida.getContentAsString()).get("id_respuesta").asLong();
    }
    @AfterEach void limpiar() {
        STT.alRecibir=()->{};
        jdbc.update("DELETE FROM sesiones_entrevista WHERE id_usuario=?",usuario);
        jdbc.update("DELETE FROM usuarios WHERE id_usuario=?",usuario);
    }
    @AfterAll static void cerrar() { S3.close();STT.close(); }
    String auth() { return "Bearer "+token; }
    RespuestaVideo respuesta() { return repo.conRespuestaBloqueada(id,d->d.respuesta()).orElseThrow(); }
    ContextoProcesamiento contexto() { return new ContextoProcesamiento(respuesta()); }
    void cambiar(java.util.function.UnaryOperator<RespuestaVideo> fn) {
        repo.conRespuestaBloqueada(id,d->{repo.actualizar(fn.apply(d.respuesta()));return true;});
    }
    ResultActions estado() throws Exception { return mvc.perform(get("/api/v1/respuestas/{id}",id).header("Authorization",auth())); }
    ResultActions manual() throws Exception { return mvc.perform(post("/api/v1/respuestas/{id}/reintento",id).header("Authorization",auth())); }
    void agotarStt() { STT.estado=503; for(int i=0;i<3;i++) { assertThat(worker.procesarSiguiente()).isTrue(); reloj.avanzar(16); } }
    @Test void flujoCompletoRealPrivadoSTTNullMetYLimiteIncremento7() throws Exception {
        assertThat(worker.procesarSiguiente()).isTrue();
        assertThat(contexto().etapa()).isEqualTo(ContextoProcesamiento.Etapa.TRANSCRIBIR);
        assertThat(contexto().audio()).isEqualTo("usuarios/"+usuario+"/sesiones/"+sesion+"/preguntas/"+pregunta+"/audio.wav");
        assertThat(S3.objetos.get("/pruebas/"+contexto().audio()).tipo()).isEqualTo("audio/wav");
        assertThat(worker.procesarSiguiente()).isTrue();
        var t=repo.buscarTranscripcion(id).orElseThrow();
        assertThat(t.confianzaStt()).isNull();assertThat(t.idioma()).isEqualTo("es");
        assertThat(t.fechaTranscripcion()).isEqualTo(LocalDateTime.now(reloj));
        assertThat(worker.procesarSiguiente()).isTrue();
        var m=repo.buscarMetrica(id).orElseThrow();
        assertThat(m.conteoPalabras()).isEqualTo(7);assertThat(m.conteoMuletillas()).isEqualTo(3);
        assertThat(m.versionMetricas()).isEqualTo("MET-1.0");
        assertThat(worker.procesarSiguiente()).isFalse();
        estado().andExpect(status().isOk()).andExpect(jsonPath("$.estado_procesamiento").value("TRANSCRITO"))
            .andExpect(jsonPath("$.etapa_actual").value("EVALUAR")).andExpect(jsonPath("$.metricas.conteo_palabras").value(7))
            .andExpect(jsonPath("$.puede_reintentar").value(false));
        assertThat(respuesta().procesamientoContexto()).containsEntry("bloqueo_hasta_utc",null).containsEntry("proximo_intento_utc",null).containsEntry("ultimo_error",null);
        assertThat(STT.peticiones).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evaluaciones_star",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT estado_sesion FROM sesiones_entrevista WHERE id_sesion=?",String.class,sesion)).isEqualTo("EN_PROCESO");
        assertThat(S3.peticiones).allMatch(S3HttpFixture.Peticion::firmada);
    }
    @Test void ciclo1ManualCiclo2ExitoSegundoManual409YPropiedad404() throws Exception {
        worker.procesarSiguiente();agotarStt();
        estado().andExpect(jsonPath("$.estado_procesamiento").value("FALLIDO")).andExpect(jsonPath("$.puede_reintentar").value(true))
            .andExpect(jsonPath("$.estado_sesion").value("EN_PROCESO"));
        mvc.perform(post("/api/v1/respuestas/{id}/reintento",id)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/respuestas/{id}/reintento",99999999).header("Authorization",auth())).andExpect(status().isNotFound());
        manual().andExpect(status().isAccepted()).andExpect(jsonPath("$.estado_procesamiento").value("AUDIO_EXTRAIDO")).andExpect(jsonPath("$.puede_reintentar").value(false));
        manual().andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("REINTENTO_NO_DISPONIBLE"));
        STT.estado=200;worker.procesarSiguiente();worker.procesarSiguiente();
        assertThat(contexto().ciclo()).isEqualTo(2);assertThat(contexto().etapa()).isEqualTo(ContextoProcesamiento.Etapa.EVALUAR);
        assertThat(S3.peticiones.stream().filter(p->p.metodo().equals("PUT")&&p.clave().endsWith("audio.wav")).count()).isEqualTo(1);
        manual().andExpect(status().isConflict());assertThat(STT.peticiones).hasSize(4);
    }
    @Test void segundoCicloAgotadoCancelaAtomicoYNoAdmiteMasTrabajo() throws Exception {
        worker.procesarSiguiente();agotarStt();manual().andExpect(status().isAccepted());agotarStt();
        estado().andExpect(jsonPath("$.estado_sesion").value("CANCELADA")).andExpect(jsonPath("$.puede_reintentar").value(false));
        assertThat(contexto().definitivo()).isTrue();assertThat(STT.peticiones).hasSize(6);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sesiones_entrevista WHERE id_sesion=? AND fecha_fin>=fecha_inicio AND puntaje_promedio IS NULL",Integer.class,sesion)).isEqualTo(1);
        assertThat(worker.procesarSiguiente()).isFalse();manual().andExpect(status().isConflict());
    }
    @Test void sttVacioCancelaSinReintentos() throws Exception {
        worker.procesarSiguiente();STT.texto="   ";worker.procesarSiguiente();
        assertThat(contexto().definitivo()).isTrue();assertThat(repo.buscarTranscripcion(id)).isEmpty();
        estado().andExpect(jsonPath("$.estado_sesion").value("CANCELADA")).andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("no recuperable")));
        assertThat(STT.peticiones).hasSize(1);assertThat(worker.procesarSiguiente()).isFalse();
    }
    @Test void sinPalabrasCancelaEnMetricasPreservandoTranscripcion() throws Exception {
        STT.texto="... -- 😀";worker.procesarSiguiente();worker.procesarSiguiente();worker.procesarSiguiente();
        assertThat(contexto().definitivo()).isTrue();assertThat(repo.buscarTranscripcion(id)).isPresent();assertThat(repo.buscarMetrica(id)).isEmpty();
        estado().andExpect(jsonPath("$.estado_sesion").value("CANCELADA"));
    }
    @Test void restartLeaseExpiradoReanudaSttSinExtraerOtraVez() {
        worker.procesarSiguiente();
        var perdido=worker.reclamarSiguiente().orElseThrow();
        assertThat(contexto().intentos()).isEqualTo(1);assertThat(worker.reclamarSiguiente()).isEmpty();
        reloj.avanzar(90);assertThat(worker.reclamarSiguiente()).isEmpty();
        reloj.avanzar(1);
        var reiniciado=new ProcesarRespuestasService(repo,almacenamiento,extractor,stt,accion->()->{},reloj,5,15);
        assertThat(reiniciado.procesarSiguiente()).isTrue();
        assertThat(contexto().etapa()).isEqualTo(ContextoProcesamiento.Etapa.METRICAS);
        worker.ejecutar(perdido); // El resultado viejo no puede sobrescribir la versión nueva.
        assertThat(contexto().etapa()).isEqualTo(ContextoProcesamiento.Etapa.METRICAS);
        assertThat(repo.buscarTranscripcion(id)).isPresent();
        assertThat(S3.peticiones.stream().filter(p->p.metodo().equals("PUT")&&p.clave().endsWith("audio.wav")).count()).isEqualTo(1);
        assertThat(contexto().version()).isEqualTo(3);
    }
    @Test void tercerClaimCaidoAgotaSinCuartaOperacion() throws Exception {
        worker.procesarSiguiente();
        for(int i=0;i<3;i++) { assertThat(worker.reclamarSiguiente()).isPresent();reloj.avanzar(91); }
        assertThat(worker.procesarSiguiente()).isFalse();assertThat(STT.peticiones).isEmpty();
        assertThat(contexto().intentos()).isEqualTo(3);
        estado().andExpect(jsonPath("$.puede_reintentar").value(true));
        manual().andExpect(status().isAccepted());
        for(int i=0;i<3;i++) { assertThat(worker.reclamarSiguiente()).isPresent();reloj.avanzar(91); }
        assertThat(worker.procesarSiguiente()).isFalse();
        estado().andExpect(jsonPath("$.estado_sesion").value("CANCELADA"));
    }
    @Test void heartbeatImpideRoboYNoResucitaLeaseCompletado() {
        worker.procesarSiguiente();var job=worker.reclamarSiguiente().orElseThrow();
        reloj.avanzar(80);worker.renovarLease(job);reloj.avanzar(11);
        assertThat(worker.reclamarSiguiente()).isEmpty();worker.ejecutar(job);
        worker.renovarLease(job);assertThat(contexto().tieneLease()).isFalse();
        assertThat(contexto().etapa()).isEqualTo(ContextoProcesamiento.Etapa.METRICAS);
    }
    @Test void cancelacionDuranteSttLiberaSinPersistirResultadoNiAlterarTerminal() throws Exception {
        worker.procesarSiguiente();var job=worker.reclamarSiguiente().orElseThrow();
        STT.alRecibir=()->{
            repo.conRespuestaBloqueada(id,d->{repo.actualizarSesion(d.sesion().finalizarAnticipadamente(LocalDateTime.now(reloj)));return true;});
        };
        worker.ejecutar(job);
        assertThat(contexto().tieneLease()).isFalse();assertThat(repo.buscarTranscripcion(id)).isEmpty();
        estado().andExpect(jsonPath("$.estado_sesion").value("CANCELADA")).andExpect(jsonPath("$.estado_procesamiento").value("AUDIO_EXTRAIDO"));
    }
    @Test void idempotenciaHitosAudioYTranscripcionYUpsertMetrica() {
        worker.procesarSiguiente();
        cambiar(r->{var map=new LinkedHashMap<>(r.procesamientoContexto());map.put("etapa_actual","EXTRAER_AUDIO");return copia(r,map);});
        long puts=S3.peticiones.stream().filter(p->p.metodo().equals("PUT")).count();
        worker.procesarSiguiente();assertThat(S3.peticiones.stream().filter(p->p.metodo().equals("PUT")).count()).isEqualTo(puts);
        worker.procesarSiguiente();int llamadas=STT.peticiones.size();
        cambiar(r->{var map=new LinkedHashMap<>(r.procesamientoContexto());map.put("etapa_actual","TRANSCRIBIR");return copia(r,map);});
        worker.procesarSiguiente();assertThat(STT.peticiones).hasSize(llamadas);
        worker.procesarSiguiente();
        cambiar(r->{var map=new LinkedHashMap<>(r.procesamientoContexto());map.put("etapa_actual","METRICAS");return copia(r,map);});
        worker.procesarSiguiente();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM metricas_comunicacion WHERE id_respuesta=?",Integer.class,id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM transcripciones WHERE id_respuesta=?",Integer.class,id)).isEqualTo(1);
    }
    @Test void claimsConcurrentesSoloUnoObtieneFila() throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        try {
            var a=pool.submit(()->worker.reclamarSiguiente());var b=pool.submit(()->worker.reclamarSiguiente());
            assertThat(java.util.stream.Stream.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS)).filter(Optional::isPresent).count()).isEqualTo(1);
            assertThat(contexto().intentos()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    RespuestaVideo copia(RespuestaVideo r,Map<String,Object> map) {
        return new RespuestaVideo(r.idRespuesta(),r.idSesion(),r.idPregunta(),r.publicIdCloud(),r.duracionSegundos(),r.formato(),r.tamanoBytes(),r.estadoProcesamiento(),r.fechaGrabacion(),map);
    }
}

