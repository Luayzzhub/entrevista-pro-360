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
@Import(EvaluacionIntegrationTest.Config.class)
class EvaluacionIntegrationTest {
    static final S3HttpFixture S3=new S3HttpFixture();
    static final SttHttpFixture STT=new SttHttpFixture();
    static final LlmHttpFixture LLM=new LlmHttpFixture();
    static final class Reloj extends Clock {
        volatile Instant instante=Instant.parse("2026-10-25T18:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { if(!zone.equals(ZoneOffset.UTC)) throw new IllegalArgumentException(); return this; }
        public Instant instant() { return instante; }
        void avanzar(long segundos) { instante=instante.plusSeconds(segundos); }
    }
    @TestConfiguration static class Config {
        @Bean @Primary S3Client s3Local() { return S3.cliente(); }
        @Bean @Primary software.amazon.awssdk.auth.credentials.AwsCredentialsProvider credenciales(){return software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("ep360-local-test","ep360-local-test-secret"));}
        @Bean @Primary Reloj reloj() { return new Reloj(); }
    }
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r) {
        r.add("integraciones.s3-bucket",()->"pruebas");
        r.add("integraciones.s3-endpoint-override",()->S3.endpoint().toString());
        r.add("integraciones.llm-model",()->"gpt-5.4-mini-2026-03-17");
        r.add("integraciones.openai-base-url",LLM::endpoint); r.add("integraciones.openai-api-key",()->"clave-local");
        r.add("integraciones.stt-model",()->"gpt-4o-mini-transcribe-2025-12-15");
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired ProcesarRespuestasService worker; @Autowired ProcesamientoRepository repo;
    @Autowired AlmacenamientoVideoPort almacenamiento; @Autowired ExtractorAudioPort extractor;
    @Autowired TranscripcionSttPort stt; @Autowired Reloj reloj;
    String token; long usuario,sesion,id; int pregunta;
    @BeforeEach void preparar() throws Exception {
        S3.limpiar();STT.limpiar();LLM.limpiar();LLM.sttEndpoint=STT.endpoint();S3.relojFirma=reloj;reloj.instante=Instant.parse("2026-10-25T18:00:00Z");
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
        STT.alRecibir=()->{};LLM.alRecibir=()->{};
        jdbc.update("DELETE FROM sesiones_entrevista WHERE id_usuario=?",usuario);
        jdbc.update("DELETE FROM usuarios WHERE id_usuario=?",usuario);
    }
    @AfterAll static void cerrar() { S3.close();STT.close();LLM.close(); }
    String auth() { return "Bearer "+token; }
    RespuestaVideo respuesta() { return repo.conRespuestaBloqueada(id,d->d.respuesta()).orElseThrow(); }
    ContextoProcesamiento contexto() { return new ContextoProcesamiento(respuesta()); }
    void cambiar(java.util.function.UnaryOperator<RespuestaVideo> fn) {
        repo.conRespuestaBloqueada(id,d->{repo.actualizar(fn.apply(d.respuesta()));return true;});
    }
    ResultActions estado() throws Exception { return mvc.perform(get("/api/v1/respuestas/{id}",id).header("Authorization",auth())); }
    ResultActions manual() throws Exception { return mvc.perform(post("/api/v1/respuestas/{id}/reintento",id).header("Authorization",auth())); }
    void agotarStt() { STT.estado=503; for(int i=0;i<3;i++) { assertThat(worker.procesarSiguiente()).isTrue(); reloj.avanzar(16); } }


    void puntajes(int... puntos) {
        LLM.generarSalida=aplica->{
            var s=EvaluacionFixtures.salida(aplica);
            @SuppressWarnings("unchecked") var criterios=(Map<String,Object>)s.get("criterios");
            for(int i=0;i<4;i++) {
                @SuppressWarnings("unchecked") var c=(Map<String,Object>)criterios.get(ValidadorEvaluacion.CRITERIOS.get(i));
                c.put("puntaje",puntos[i]);
            }
            return s;
        };
    }
    List<Long> primerasDosCompletas()throws Exception {
        puntajes(75,100,50,75);evaluar();long primera=id;
        id=subirPregunta(2);puntajes(75,100,75,75);evaluar();long segunda=id;
        id=subirPregunta(3);return List.of(primera,segunda);
    }
    ResultActions detalleSesion()throws Exception {return mvc.perform(get("/api/v1/sesiones/{id}",sesion).header("Authorization",auth()));}
    @Test void promedio8125CierreAtomicoFechaUtc409CancelacionNuevaPracticaYPanel()throws Exception {
        primerasDosCompletas();puntajes(75,100,75,100);evaluar();
        var cierre=detalleSesion().andExpect(status().isOk()).andExpect(jsonPath("$.estado_sesion").value("COMPLETADA"))
                .andExpect(jsonPath("$.puntaje_promedio").value(81.25)).andExpect(jsonPath("$.preguntas[0].puntaje_general").value(75.00))
                .andExpect(jsonPath("$.preguntas[1].puntaje_general").value(81.25)).andExpect(jsonPath("$.preguntas[2].puntaje_general").value(87.50))
                .andExpect(jsonPath("$.preguntas[2].version_rubrica").value("COM-1.0"))
                .andExpect(jsonPath("$.preguntas[2].puede_reintentar").value(false))
                .andExpect(jsonPath("$.pregunta_habilitada").doesNotExist()).andReturn().getResponse().getContentAsString();
        var nodo=json.readTree(cierre);String fecha=nodo.get("fecha_fin").asText();
        assertThat(fecha).endsWith("Z");assertThat(Instant.parse(fecha)).isEqualTo(reloj.instant());
        assertThat(jdbc.queryForObject("SELECT puntaje_promedio FROM sesiones_entrevista WHERE id_sesion=?",java.math.BigDecimal.class,sesion)).isEqualTo(new java.math.BigDecimal("81.25"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sesiones_entrevista WHERE id_sesion=? AND fecha_fin>=fecha_inicio",Integer.class,sesion)).isEqualTo(1);
        mvc.perform(post("/api/v1/sesiones/{id}/cancelacion",sesion).header("Authorization",auth()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("SESION_YA_COMPLETADA"));
        detalleSesion().andExpect(jsonPath("$.puntaje_promedio").value(81.25)).andExpect(jsonPath("$.fecha_fin").value(fecha));
        mvc.perform(get("/api/v1/sesiones").header("Authorization",auth())).andExpect(status().isOk())
                .andExpect(jsonPath("$.ultima_sesion_completada.id_sesion").value(sesion))
                .andExpect(jsonPath("$.ultima_sesion_completada.puntaje_promedio").value(81.25))
                .andExpect(jsonPath("$.total_sesiones_completadas").value(1));
        int puesto=jdbc.queryForObject("SELECT id_puesto FROM sesiones_entrevista WHERE id_sesion=?",Integer.class,sesion);
        mvc.perform(post("/api/v1/sesiones").header("Authorization",auth()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("id_puesto",puesto)))).andExpect(status().isCreated());
    }
    @Test void promedioRedondeado208ConCerosReales()throws Exception {
        puntajes(0,0,0,0);evaluar();id=subirPregunta(2);evaluar();id=subirPregunta(3);puntajes(0,0,0,25);evaluar();
        detalleSesion().andExpect(jsonPath("$.estado_sesion").value("COMPLETADA")).andExpect(jsonPath("$.puntaje_promedio").value(2.08));
        assertThat(jdbc.queryForObject("SELECT puntaje_promedio FROM sesiones_entrevista WHERE id_sesion=?",java.math.BigDecimal.class,sesion)).isEqualTo(new java.math.BigDecimal("2.08"));
    }
    @Test void promedioCeroConTresEvaluacionesValidasCierra()throws Exception {
        puntajes(0,0,0,0);evaluar();id=subirPregunta(2);evaluar();id=subirPregunta(3);evaluar();
        detalleSesion().andExpect(jsonPath("$.estado_sesion").value("COMPLETADA")).andExpect(jsonPath("$.puntaje_promedio").value(0.00));
    }
    @Test void p3Definitiva401ConservaDosEvaluacionesYPublicaMotivoSeguro()throws Exception {
        var anteriores=primerasDosCompletas();primerasTres();LLM.estado=401;worker.procesarSiguiente();
        assertThat(repo.buscarEvaluacion(anteriores.get(0))).isPresent();assertThat(repo.buscarEvaluacion(anteriores.get(1))).isPresent();
        assertThat(repo.buscarEvaluacion(id)).isEmpty();
        var body=detalleSesion().andExpect(jsonPath("$.estado_sesion").value("CANCELADA"))
                .andExpect(jsonPath("$.puntaje_promedio").doesNotExist()).andExpect(jsonPath("$.fecha_fin").exists())
                .andExpect(jsonPath("$.respuestas_evaluadas").value(2))
                .andExpect(jsonPath("$.motivo_cierre").value("La sesión se canceló por una falla definitiva al procesar una respuesta."))
                .andExpect(jsonPath("$.preguntas[2].motivo_fallo").exists()).andExpect(jsonPath("$.preguntas[2].puede_reintentar").value(false))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("LLM_HTTP_401","public_id_cloud","referencia_audio","procesamiento_contexto");
    }
    @Test void p3AgotamientoManualCancelaYConservaP1P2()throws Exception {
        var anteriores=primerasDosCompletas();primerasTres();LLM.estado=503;fallarLlm();
        detalleSesion().andExpect(jsonPath("$.estado_sesion").value("EN_PROCESO")).andExpect(jsonPath("$.puntaje_promedio").doesNotExist())
                .andExpect(jsonPath("$.fecha_fin").doesNotExist()).andExpect(jsonPath("$.motivo_cierre").doesNotExist())
                .andExpect(jsonPath("$.preguntas[2].puede_reintentar").value(true));
        manual().andExpect(status().isAccepted());fallarLlm();
        detalleSesion().andExpect(jsonPath("$.estado_sesion").value("CANCELADA")).andExpect(jsonPath("$.puntaje_promedio").doesNotExist())
                .andExpect(jsonPath("$.respuestas_evaluadas").value(2));
        for(long previa:anteriores)assertThat(repo.buscarEvaluacion(previa)).isPresent();
    }
    @Test void cancelacionConcurrenteDuranteLlmP3GanaSinPublicarTerceraNiPromedio()throws Exception {
        primerasDosCompletas();primerasTres();
        var trabajo=worker.reclamarSiguiente().orElseThrow();
        var iniciada=new java.util.concurrent.CountDownLatch(1);var liberar=new java.util.concurrent.CountDownLatch(1);
        LLM.alRecibir=()->{
            iniciada.countDown();
            try{if(!liberar.await(15,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("La prueba no liberó LLM.");}
            catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IllegalStateException(ex);}
        };
        var pool=java.util.concurrent.Executors.newSingleThreadExecutor();
        try{
            var futuro=pool.submit(()->worker.ejecutar(trabajo));
            assertThat(iniciada.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            mvc.perform(post("/api/v1/sesiones/{id}/cancelacion",sesion).header("Authorization",auth())).andExpect(status().isOk());
            liberar.countDown();futuro.get(10,java.util.concurrent.TimeUnit.SECONDS);
        }finally{liberar.countDown();pool.shutdownNow();LLM.alRecibir=()->{};}
        assertThat(repo.buscarEvaluacion(id)).isEmpty();assertThat(contexto().tieneLease()).isFalse();
        detalleSesion().andExpect(jsonPath("$.estado_sesion").value("CANCELADA")).andExpect(jsonPath("$.puntaje_promedio").doesNotExist())
                .andExpect(jsonPath("$.respuestas_evaluadas").value(2)).andExpect(jsonPath("$.motivo_cierre").value("La sesión fue finalizada anticipadamente por el postulante."));
    }
    @Test void noCierraSiFaltaUnaTranscripcionAunqueLasTresRespuestasSeanEvaluadas()throws Exception {
        var anteriores=primerasDosCompletas();
        jdbc.update("DELETE FROM transcripciones WHERE id_respuesta=?",anteriores.get(0));
        evaluar();
        assertThat(repo.buscarEvaluacion(id)).isPresent();
        detalleSesion().andExpect(jsonPath("$.estado_sesion").value("EN_PROCESO")).andExpect(jsonPath("$.puntaje_promedio").doesNotExist())
                .andExpect(jsonPath("$.fecha_fin").doesNotExist()).andExpect(jsonPath("$.respuestas_evaluadas").value(3));
    }
    @Test void resumenCanceladoVoluntarioTrasUnaRespuestaNoInventaPuntajes()throws Exception {
        evaluar();
        mvc.perform(post("/api/v1/sesiones/{id}/cancelacion",sesion).header("Authorization",auth())).andExpect(status().isOk());
        var r=detalleSesion().andExpect(jsonPath("$.respuestas_evaluadas").value(1)).andExpect(jsonPath("$.puntaje_promedio").doesNotExist())
                .andExpect(jsonPath("$.preguntas[0].puntaje_general").value(75.00))
                .andExpect(jsonPath("$.preguntas[1].puntaje_general").doesNotExist()).andExpect(jsonPath("$.preguntas[1].id_respuesta").doesNotExist())
                .andExpect(jsonPath("$.preguntas[2].version_rubrica").doesNotExist()).andExpect(jsonPath("$.preguntas[2].motivo_fallo").doesNotExist())
                .andExpect(jsonPath("$.motivo_cierre").value("La sesión fue finalizada anticipadamente por el postulante."))
                .andReturn().getResponse().getContentAsString();
        assertThat(r).doesNotContain("public_id_cloud","referencia_audio","procesamiento_contexto");
    }
    void primerasTres(){for(int i=0;i<3;i++)assertThat(worker.procesarSiguiente()).isTrue();assertThat(contexto().etapa()).isEqualTo(ContextoProcesamiento.Etapa.EVALUAR);}
    void evaluar(){primerasTres();assertThat(worker.procesarSiguiente()).isTrue();}
    void fallarLlm(){for(int i=0;i<3;i++){assertThat(worker.procesarSiguiente()).isTrue();reloj.avanzar(16);}}
    long subirPregunta(int orden)throws Exception{
        int p=jdbc.queryForObject("SELECT p.id_pregunta FROM preguntas p JOIN sesiones_entrevista s ON s.id_puesto=p.id_puesto WHERE s.id_sesion=? AND p.orden=?",Integer.class,sesion,orden);
        var r=mvc.perform(multipart(HttpMethod.PUT,"/api/v1/sesiones/{id}/preguntas/{p}/respuesta",sesion,p)
                .file(new MockMultipartFile("video","toma.webm","video/webm",Files.readAllBytes(MediaFixtures.archivo("webm")))).header("Authorization",auth()))
                .andExpect(status().isAccepted()).andReturn().getResponse();
        return json.readTree(r.getContentAsString()).get("id_respuesta").asLong();
    }
    @Test void cuatroEtapasP1NoStarP2StarYTercceraCompletaSesion()throws Exception{
        evaluar();
        assertThat(contexto().etapa()).isEqualTo(ContextoProcesamiento.Etapa.FINALIZADO);
        assertThat(respuesta().estadoProcesamiento()).isEqualTo(RespuestaVideo.EstadoProcesamiento.EVALUADO);
        var e=repo.buscarEvaluacion(id).orElseThrow();assertThat(e.puntajeGeneral()).isEqualByComparingTo("75.00");
        assertThat(e.detalleStar()).isNull();assertThat(e.aplicaStar()).isFalse();
        assertThat(worker.procesarSiguiente()).isFalse();
        estado().andExpect(jsonPath("$.evaluacion.criterios.claridad.puntaje").value(100.00))
                .andExpect(jsonPath("$.evaluacion.criterios.claridad.justificacion").value("Justificación de claridad"))
                .andExpect(jsonPath("$.evaluacion.version_contrato").value("EVAL-1.0"))
                .andExpect(jsonPath("$.transcripcion.idioma").value("es")).andExpect(jsonPath("$.metricas.detalle_muletillas.eh").value(1))
                .andExpect(jsonPath("$.evaluacion.detalle_star").doesNotExist());
        mvc.perform(get("/api/v1/sesiones/{id}",sesion).header("Authorization",auth()))
                .andExpect(jsonPath("$.pregunta_habilitada.orden").value(2));
        id=subirPregunta(2);evaluar();
        assertThat(repo.buscarEvaluacion(id).orElseThrow().detalleStar()).containsOnlyKeys("situacion","tarea","accion","resultado");
        estado().andExpect(jsonPath("$.evaluacion.aplica_star").value(true)).andExpect(jsonPath("$.evaluacion.detalle_star.accion.estado").value("PRESENTE"));
        mvc.perform(get("/api/v1/sesiones/{id}",sesion).header("Authorization",auth())).andExpect(jsonPath("$.pregunta_habilitada.orden").value(3));
        id=subirPregunta(3);evaluar();
        mvc.perform(get("/api/v1/sesiones/{id}",sesion).header("Authorization",auth()))
            .andExpect(jsonPath("$.estado_sesion").value("COMPLETADA")).andExpect(jsonPath("$.puntaje_promedio").value(75.00))
            .andExpect(jsonPath("$.fecha_fin").exists()).andExpect(jsonPath("$.respuestas_evaluadas").value(3))
            .andExpect(jsonPath("$.pregunta_habilitada").doesNotExist());
        assertThat(LLM.peticiones).hasSize(3);assertThat(STT.peticiones).hasSize(3);
    }
    @Test void tres503ManualSoloEvaluarConservaAudioSttMetYSegundoManual409()throws Exception{
        primerasTres();var metrica=repo.buscarMetrica(id).orElseThrow();int peticiones=S3.peticiones.size();
        LLM.estado=503;fallarLlm();
        assertThat(repo.buscarEvaluacion(id)).isEmpty();
        estado().andExpect(jsonPath("$.puede_reintentar").value(true)).andExpect(jsonPath("$.ultimo_hito_valido").value("TRANSCRITO"))
            .andExpect(jsonPath("$.estado_sesion").value("EN_PROCESO"));
        manual().andExpect(status().isAccepted()).andExpect(jsonPath("$.estado_procesamiento").value("TRANSCRITO")).andExpect(jsonPath("$.etapa_actual").value("EVALUAR"));
        LLM.estado=200;worker.procesarSiguiente();
        assertThat(repo.buscarMetrica(id).orElseThrow().idMetrica()).isEqualTo(metrica.idMetrica());
        assertThat(S3.peticiones).hasSize(peticiones);assertThat(STT.peticiones).hasSize(1);assertThat(LLM.peticiones).hasSize(4);
        assertThat(contexto().etapa()).isEqualTo(ContextoProcesamiento.Etapa.FINALIZADO);
        manual().andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("REINTENTO_NO_DISPONIBLE"));
    }
    @Test void salidaInvalidaAgotaCiclo1SinFilasLuegoCiclo2Exito()throws Exception{
        primerasTres();LLM.modo="json_invalido";fallarLlm();
        assertThat(repo.buscarEvaluacion(id)).isEmpty();assertThat(contexto().recuperable()).isTrue();
        assertThat(((Map<?,?>)respuesta().procesamientoContexto().get("ultimo_error")).get("codigo")).isEqualTo("SALIDA_EVALUACION_INVALIDA");
        manual().andExpect(status().isAccepted());LLM.modo="correcto";worker.procesarSiguiente();
        assertThat(repo.buscarEvaluacion(id)).isPresent();assertThat(STT.peticiones).hasSize(1);
    }
    @Test void ciclo2AgotadoCancelaSinPromedioNiEvaluacion()throws Exception{
        primerasTres();LLM.estado=503;fallarLlm();manual().andExpect(status().isAccepted());fallarLlm();
        assertThat(contexto().definitivo()).isTrue();assertThat(repo.buscarEvaluacion(id)).isEmpty();
        estado().andExpect(jsonPath("$.estado_sesion").value("CANCELADA")).andExpect(jsonPath("$.puede_reintentar").value(false));
        assertThat(LLM.peticiones).hasSize(6);assertThat(STT.peticiones).hasSize(1);
    }
    @Test void rechazo401CancelaInmediato()throws Exception{
        primerasTres();LLM.estado=401;worker.procesarSiguiente();
        assertThat(LLM.peticiones).hasSize(1);assertThat(contexto().definitivo()).isTrue();
        assertThat(repo.buscarEvaluacion(id)).isEmpty();estado().andExpect(jsonPath("$.estado_sesion").value("CANCELADA"));
    }
    @Test void cancelacionDuranteLlmYFencingNoPersistenResultado()throws Exception{
        primerasTres();var viejo=worker.reclamarSiguiente().orElseThrow();reloj.avanzar(91);
        var vigente=worker.reclamarSiguiente().orElseThrow();worker.ejecutar(viejo);
        assertThat(repo.buscarEvaluacion(id)).isEmpty();assertThat(contexto().version()).isEqualTo(vigente.version());
        LLM.alRecibir=()->repo.conRespuestaBloqueada(id,d->{repo.actualizarSesion(d.sesion().finalizarAnticipadamente(LocalDateTime.now(reloj)));return true;});
        worker.ejecutar(vigente);assertThat(repo.buscarEvaluacion(id)).isEmpty();assertThat(contexto().tieneLease()).isFalse();
        estado().andExpect(jsonPath("$.estado_sesion").value("CANCELADA"));
    }
    @Test void idempotenciaFilaValidaNoInvocaLlmYFinalizadoNoSeReclama(){
        primerasTres();
        repo.conRespuestaBloqueada(id,d->{repo.guardarEvaluacion(ValidadorEvaluacion.validar(id,EvaluacionFixtures.salida(false),false,LocalDateTime.now(reloj)));return true;});
        worker.procesarSiguiente();assertThat(LLM.peticiones).isEmpty();assertThat(worker.procesarSiguiente()).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evaluaciones_star WHERE id_respuesta=?",Integer.class,id)).isEqualTo(1);
    }
    @Test void leaseTercerIntentoEvaluarCaidoNoEjecutaCuarto()throws Exception{
        primerasTres();for(int i=0;i<3;i++){worker.reclamarSiguiente().orElseThrow();reloj.avanzar(91);}
        assertThat(worker.procesarSiguiente()).isFalse();assertThat(LLM.peticiones).isEmpty();
        estado().andExpect(jsonPath("$.puede_reintentar").value(true));
    }
    @Test void reproduccionFirmaReal300sCacheNoStoreTamperExpiracionYVideoAusenteConservaResultado()throws Exception{
        evaluar();
        var r=mvc.perform(post("/api/v1/respuestas/{id}/reproduccion",id).header("Authorization",auth()))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.public_id_cloud").doesNotExist()).andReturn().getResponse();
        var n=json.readTree(r.getContentAsString());String url=n.get("video_url_cloud").asText();
        assertThat(url).contains("X-Amz-Expires=300","X-Amz-Signature=");
        assertThat(Instant.parse(n.get("expira_en").asText())).isEqualTo(reloj.instant().plusSeconds(300));
        var http=java.net.http.HttpClient.newHttpClient();
        var content=http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofByteArray());
        assertThat(content.statusCode()).isEqualTo(200);assertThat(content.body()).isEqualTo(Files.readAllBytes(MediaFixtures.archivo("webm")));
        String corrupta=url.replace("X-Amz-Expires=300","X-Amz-Expires=301");
        assertThat(http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(corrupta)).GET().build(),java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(403);
        reloj.avanzar(300);
        assertThat(http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).GET().build(),java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(403);
        S3.objetos.remove("/pruebas/"+respuesta().publicIdCloud());
        mvc.perform(post("/api/v1/respuestas/{id}/reproduccion",id).header("Authorization",auth())).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("VIDEO_NO_DISPONIBLE"));
        estado().andExpect(jsonPath("$.evaluacion.puntaje_general").value(75.00)).andExpect(jsonPath("$.transcripcion.texto_transcrito").exists());
        assertThat(repo.buscarEvaluacion(id)).isPresent();
    }
    @Test void reproduccionS3Caido503YAjeno404SinTocarHistorico()throws Exception{
        evaluar();S3.noDisponible=true;
        mvc.perform(post("/api/v1/respuestas/{id}/reproduccion",id).header("Authorization",auth())).andExpect(status().isServiceUnavailable());
        S3.noDisponible=false;
        String email="ajeno-"+UUID.randomUUID()+"@example.test",pass="password-ajeno-2026";
        var registro=mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("nombre_completo","Ajeno","email",email,"password",pass)))).andExpect(status().isCreated()).andReturn().getResponse();
        long otro=json.readTree(registro.getContentAsString()).get("id_usuario").asLong();
        try{
            var login=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email",email,"password",pass)))).andExpect(status().isOk()).andReturn().getResponse();
            String ajeno=json.readTree(login.getContentAsString()).get("access_token").asText();
            mvc.perform(post("/api/v1/respuestas/{id}/reproduccion",id).header("Authorization","Bearer "+ajeno)).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("RESPUESTA_NO_ENCONTRADA"));
            mvc.perform(get("/api/v1/respuestas/{id}",id).header("Authorization","Bearer "+ajeno)).andExpect(status().isNotFound());
            mvc.perform(post("/api/v1/respuestas/{id}/reintento",id).header("Authorization","Bearer "+ajeno)).andExpect(status().isNotFound());
        }finally{jdbc.update("DELETE FROM usuarios WHERE id_usuario=?",otro);}
        assertThat(repo.buscarEvaluacion(id)).isPresent();
    }
}

