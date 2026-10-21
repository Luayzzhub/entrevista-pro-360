package pe.utp.entrevistapro360.presentation;

import com.fasterxml.jackson.databind.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.*;
import pe.utp.entrevistapro360.domain.RespuestaVideo;
import pe.utp.entrevistapro360.infrastructure.persistence.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class SesionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired RespuestaVideoJpaRepository respuestas;
    record Cuenta(long id, String token) { }
    private String body(Object value) throws Exception { return json.writeValueAsString(value); }
    private Cuenta cuenta() throws Exception {
        String email = "sesion-" + UUID.randomUUID() + "@example.test";
        String registro = mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("nombre_completo", "Cuenta de prueba", "email", email, "password", "password-sesion-2026"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("email", email, "password", "password-sesion-2026"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Cuenta(json.readTree(registro).get("id_usuario").asLong(), json.readTree(login).get("access_token").asText());
    }
    private int puesto() { return jdbc.queryForObject("SELECT MIN(id_puesto) FROM puestos WHERE activo=TRUE", Integer.class); }
    private JsonNode crear(Cuenta cuenta) throws Exception {
        var respuesta = mvc.perform(post("/api/v1/sesiones").header("Authorization", "Bearer " + cuenta.token()).contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("id_puesto", puesto())))).andExpect(status().isCreated()).andReturn().getResponse();
        JsonNode sesion = json.readTree(respuesta.getContentAsString());
        assertThat(respuesta.getHeader("Location")).isEqualTo("/api/v1/sesiones/" + sesion.get("id_sesion").asLong());
        return sesion;
    }
    private JsonNode consultar(Cuenta cuenta, long id) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/sesiones/{id}", id).header("Authorization", "Bearer " + cuenta.token()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private JsonNode cancelar(Cuenta cuenta, long id) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/sesiones/{id}/cancelacion", id).header("Authorization", "Bearer " + cuenta.token()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private void estado(long id, String estado, BigDecimal promedio, LocalDateTime fin) {
        jdbc.update("UPDATE sesiones_entrevista SET estado_sesion=?, puntaje_promedio=?, fecha_fin=? WHERE id_sesion=?", estado, promedio, fin, id);
        em.clear();
    }
    private void respuesta(long sesion, int orden, RespuestaVideo.EstadoProcesamiento estado) {
        int pregunta = jdbc.queryForObject("SELECT id_pregunta FROM preguntas WHERE id_puesto=? AND orden=?", Integer.class, puesto(), orden);
        respuestas.saveAndFlush(RespuestaVideoJpaEntity.nueva(new RespuestaVideo(null, sesion, pregunta, "fixture-solo-test", new BigDecimal("30.000"), "webm", 1024L,
                estado, LocalDateTime.now(Clock.systemUTC()).withNano(0), Map.of("origen", "fixture-integracion"))));
        jdbc.update("UPDATE sesiones_entrevista SET estado_sesion='EN_PROCESO' WHERE id_sesion=?", sesion); em.clear();
    }

    @Test void crearDevuelve201LocationPropietarioFechasUtcYPrimeraPregunta() throws Exception {
        Cuenta c = cuenta(); JsonNode s = crear(c);
        assertThat(s.get("id_usuario").asLong()).isEqualTo(c.id());
        assertThat(s.get("estado_sesion").asText()).isEqualTo("INICIADA");
        assertThat(s.get("fecha_inicio").asText()).endsWith("Z");
        LocalDateTime almacenada = jdbc.queryForObject("SELECT fecha_inicio FROM sesiones_entrevista WHERE id_sesion=?", LocalDateTime.class, s.get("id_sesion").asLong());
        assertThat(almacenada).isEqualTo(LocalDateTime.ofInstant(Instant.parse(s.get("fecha_inicio").asText()), ZoneOffset.UTC));
        assertThat(s.get("fecha_fin").isNull()).isTrue(); assertThat(s.get("puntaje_promedio").isNull()).isTrue();
        assertThat(s.get("total_preguntas").asInt()).isEqualTo(3); assertThat(s.get("respuestas_confirmadas").asInt()).isZero();
        assertThat(s.get("pregunta_habilitada").get("orden").asInt()).isEqualTo(1);
        assertThat(s.get("preguntas").size()).isEqualTo(3);
        assertThat(s.get("preguntas").get(0).get("habilitada").asBoolean()).isTrue();
        assertThat(s.get("preguntas").get(1).get("habilitada").asBoolean()).isFalse();
    }
    @Test void todasLasRutasDeSesionesSonPrivadas() throws Exception {
        mvc.perform(post("/api/v1/sesiones").contentType(MediaType.APPLICATION_JSON).content("{\"id_puesto\":1}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/sesiones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/sesiones/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/sesiones/1/cancelacion")).andExpect(status().isUnauthorized());
    }
    @Test void idPuestoNuloNoPositivoYCamposExtraDan400() throws Exception {
        Cuenta c = cuenta();
        for (String contenido : List.of("{}", "{\"id_puesto\":null}", "{\"id_puesto\":0}", "{\"id_puesto\":-1}")) {
            mvc.perform(post("/api/v1/sesiones").header("Authorization", "Bearer " + c.token()).contentType(MediaType.APPLICATION_JSON).content(contenido))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                    .andExpect(jsonPath("$.errores_campo.id_puesto").isString());
        }
        mvc.perform(post("/api/v1/sesiones").header("Authorization", "Bearer " + c.token()).contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("id_puesto", puesto(), "id_usuario", c.id()))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("CAMPOS_NO_PERMITIDOS"));
        mvc.perform(post("/api/v1/sesiones").header("Authorization", "Bearer " + c.token()).contentType(MediaType.APPLICATION_JSON).content("{\"id_puesto\":1.9}"))
                .andExpect(status().isBadRequest());
    }
    @Test void puestoInexistenteOInactivoProduce404() throws Exception {
        Cuenta c = cuenta();
        mvc.perform(post("/api/v1/sesiones").header("Authorization", "Bearer " + c.token()).contentType(MediaType.APPLICATION_JSON).content("{\"id_puesto\":2147483647}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("PUESTO_NO_ENCONTRADO"));
        int id = puesto(); jdbc.update("UPDATE puestos SET activo=FALSE WHERE id_puesto=?", id); em.clear();
        mvc.perform(post("/api/v1/sesiones").header("Authorization", "Bearer " + c.token()).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("id_puesto", id))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("PUESTO_NO_ENCONTRADO"));
    }
    @Test void catalogoIncompletoProduce409SinCrearSesion() throws Exception {
        Cuenta c = cuenta(); int p = puesto();
        jdbc.update("UPDATE preguntas SET activo=FALSE WHERE id_puesto=? AND orden=2", p); em.clear();
        mvc.perform(post("/api/v1/sesiones").header("Authorization", "Bearer " + c.token()).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("id_puesto", p))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CATALOGO_INVALIDO"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sesiones_entrevista WHERE id_usuario=?", Integer.class, c.id())).isZero();
    }
    @Test void sesionIniciadaOEnProcesoImpideOtraYDevuelveElIdVigente() throws Exception {
        Cuenta c = cuenta(); long id = crear(c).get("id_sesion").asLong();
        for (String valor : List.of("INICIADA", "EN_PROCESO")) {
            estado(id, valor, null, null);
            mvc.perform(post("/api/v1/sesiones").header("Authorization", "Bearer " + c.token()).contentType(MediaType.APPLICATION_JSON).content(body(Map.of("id_puesto", puesto()))))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("SESION_ACTIVA_EXISTENTE"))
                    .andExpect(jsonPath("$.errores_campo.id_sesion_activa").value(Long.toString(id)));
        }
    }
    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void dosPostHttpConcurrentesPersistenUnaSolaSesionCon201Y409() throws Exception {
        Cuenta c = cuenta(); int idPuesto = puesto();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch listas = new CountDownLatch(2); CountDownLatch salida = new CountDownLatch(1);
        try {
            Callable<ResponseEntity<String>> solicitud = () -> {
                listas.countDown(); if (!salida.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("No se liberó la barrera.");
                HttpHeaders headers = new HttpHeaders(); headers.setBearerAuth(c.token()); headers.setContentType(MediaType.APPLICATION_JSON);
                return http.exchange("/api/v1/sesiones", HttpMethod.POST, new HttpEntity<>(Map.of("id_puesto", idPuesto), headers), String.class);
            };
            Future<ResponseEntity<String>> a = pool.submit(solicitud), b = pool.submit(solicitud);
            assertThat(listas.await(10, TimeUnit.SECONDS)).isTrue(); salida.countDown();
            var resultados = List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
            assertThat(resultados.stream().map(r -> r.getStatusCode().value()).toList()).containsExactlyInAnyOrder(201, 409);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sesiones_entrevista WHERE id_usuario=? AND estado_sesion IN ('INICIADA','EN_PROCESO')", Integer.class, c.id())).isEqualTo(1);
            JsonNode creada = json.readTree(resultados.stream().filter(r -> r.getStatusCode().value() == 201).findFirst().orElseThrow().getBody());
            JsonNode conflicto = json.readTree(resultados.stream().filter(r -> r.getStatusCode().value() == 409).findFirst().orElseThrow().getBody());
            assertThat(conflicto.get("errores_campo").get("id_sesion_activa").asText()).isEqualTo(creada.get("id_sesion").asText());
        } finally {
            salida.countDown(); pool.shutdownNow(); pool.awaitTermination(10, TimeUnit.SECONDS);
            jdbc.update("DELETE FROM sesiones_entrevista WHERE id_usuario=?", c.id()); jdbc.update("DELETE FROM usuarios WHERE id_usuario=?", c.id());
        }
    }
    @Test void detalleUsaRespuestasPersistidasYDesbloqueaSoloTrasEvaluado() throws Exception {
        Cuenta c = cuenta(); long id = crear(c).get("id_sesion").asLong();
        respuesta(id, 1, RespuestaVideo.EstadoProcesamiento.SUBIDO);
        var bloqueada = consultar(c, id);
        assertThat(bloqueada.get("pregunta_habilitada").isNull()).isTrue();
        assertThat(bloqueada.get("respuestas_confirmadas").asInt()).isEqualTo(1);
        assertThat(bloqueada.get("preguntas").get(0).get("estado_procesamiento").asText()).isEqualTo("SUBIDO");
        jdbc.update("UPDATE respuestas_video SET estado_procesamiento='EVALUADO' WHERE id_sesion=?", id); em.clear();
        assertThat(consultar(c, id).get("pregunta_habilitada").get("orden").asInt()).isEqualTo(2);
        respuesta(id, 2, RespuestaVideo.EstadoProcesamiento.EVALUADO);
        assertThat(consultar(c, id).get("pregunta_habilitada").get("orden").asInt()).isEqualTo(3);
        respuesta(id, 3, RespuestaVideo.EstadoProcesamiento.EVALUADO);
        var finalizada = consultar(c, id);
        assertThat(finalizada.get("pregunta_habilitada").isNull()).isTrue();
        assertThat(finalizada.get("respuestas_evaluadas").asInt()).isEqualTo(3);
        assertThat(finalizada.toString()).doesNotContain("public_id_cloud", "procesamiento_contexto", "fixture-solo-test");
    }
    @Test void sesionAjenaEInexistenteSonIndistinguiblesYNoSePuedenCancelar() throws Exception {
        Cuenta propia = cuenta(), otra = cuenta(); long id = crear(propia).get("id_sesion").asLong();
        for (long buscada : List.of(id, Long.MAX_VALUE)) {
            mvc.perform(get("/api/v1/sesiones/{id}", buscada).header("Authorization", "Bearer " + otra.token()))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("SESION_NO_ENCONTRADA"))
                    .andExpect(jsonPath("$.mensaje").value("La sesión solicitada no existe."));
            mvc.perform(post("/api/v1/sesiones/{id}/cancelacion", buscada).header("Authorization", "Bearer " + otra.token()))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("SESION_NO_ENCONTRADA"));
        }
    }
    @Test void idsInvalidosSon400EnConsultaYCancelacion() throws Exception {
        Cuenta c = cuenta();
        for (String id : List.of("0", "-1", "abc", "99999999999999999999999")) {
            mvc.perform(get("/api/v1/sesiones/{id}", id).header("Authorization", "Bearer " + c.token())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
            mvc.perform(post("/api/v1/sesiones/{id}/cancelacion", id).header("Authorization", "Bearer " + c.token())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
        }
    }
    @Test void cancelarEsIdempotenteConservaRespuestasYPermiteOtraSesion() throws Exception {
        Cuenta c = cuenta(); long id = crear(c).get("id_sesion").asLong();
        respuesta(id, 1, RespuestaVideo.EstadoProcesamiento.EVALUADO);
        var primera = cancelar(c, id);
        var segunda = cancelar(c, id);
        assertThat(primera.get("estado_sesion").asText()).isEqualTo("CANCELADA");
        assertThat(primera.get("fecha_fin").asText()).endsWith("Z");
        assertThat(segunda.get("fecha_fin")).isEqualTo(primera.get("fecha_fin"));
        assertThat(segunda.get("puntaje_promedio").isNull()).isTrue(); assertThat(segunda.get("pregunta_habilitada").isNull()).isTrue();
        assertThat(segunda.get("respuestas_evaluadas").asInt()).isEqualTo(1);
        assertThat(crear(c).get("id_sesion").asLong()).isNotEqualTo(id);
    }
    @Test void cancelacionAceptaObjetoVacioRechazaExtraYNoCancelaCompletada() throws Exception {
        Cuenta c = cuenta(); long id = crear(c).get("id_sesion").asLong();
        mvc.perform(post("/api/v1/sesiones/{id}/cancelacion", id).header("Authorization", "Bearer " + c.token()).contentType(MediaType.APPLICATION_JSON).content("{\"id_usuario\":1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("CAMPOS_NO_PERMITIDOS"));
        mvc.perform(post("/api/v1/sesiones/{id}/cancelacion", id).header("Authorization", "Bearer " + c.token()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado_sesion").value("CANCELADA"));
        long completada = crear(c).get("id_sesion").asLong(); estado(completada, "COMPLETADA", new BigDecimal("87.50"), LocalDateTime.now(Clock.systemUTC()).plusSeconds(1).withNano(0));
        mvc.perform(post("/api/v1/sesiones/{id}/cancelacion", completada).header("Authorization", "Bearer " + c.token()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("SESION_YA_COMPLETADA"));
    }
    @Test void resumenSinDatosNoInventaPromedio() throws Exception {
        Cuenta c = cuenta();
        mvc.perform(get("/api/v1/sesiones").header("Authorization", "Bearer " + c.token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sesion_pendiente").value(nullValue())).andExpect(jsonPath("$.ultima_sesion_completada").value(nullValue()))
                .andExpect(jsonPath("$.total_sesiones_completadas").value(0)).andExpect(jsonPath("$.items", hasSize(0)));
    }
    @Test void resumenEsPropioOrdenaPorInicioYDesempataUltimaCompletadaPorFin() throws Exception {
        Cuenta c = cuenta(), otra = cuenta(); LocalDateTime ahora = LocalDateTime.now(Clock.systemUTC()).withNano(0);
        long uno = crear(c).get("id_sesion").asLong(); estado(uno, "COMPLETADA", new BigDecimal("80.25"), ahora.plusMinutes(10));
        long dos = crear(c).get("id_sesion").asLong(); estado(dos, "COMPLETADA", new BigDecimal("90.00"), ahora.plusMinutes(5));
        long tres = crear(c).get("id_sesion").asLong(); cancelar(c, tres);
        long cuatro = crear(c).get("id_sesion").asLong(); crear(otra);
        jdbc.update("UPDATE sesiones_entrevista SET fecha_inicio=? WHERE id_usuario=?", ahora, c.id()); em.clear();
        var resumen = json.readTree(mvc.perform(get("/api/v1/sesiones").header("Authorization", "Bearer " + c.token()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(resumen.get("items").size()).isEqualTo(4);
        assertThat(resumen.get("sesion_pendiente").get("id_sesion").asLong()).isEqualTo(cuatro);
        assertThat(resumen.get("ultima_sesion_completada").get("id_sesion").asLong()).isEqualTo(uno);
        assertThat(resumen.get("ultima_sesion_completada").get("puntaje_promedio").decimalValue()).isEqualByComparingTo("80.25");
        assertThat(resumen.get("total_sesiones_completadas").asInt()).isEqualTo(2);
        assertThat(resumen.get("items").findValues("id_sesion").stream().map(JsonNode::asLong).toList()).containsExactly(cuatro, tres, dos, uno);
    }
}
