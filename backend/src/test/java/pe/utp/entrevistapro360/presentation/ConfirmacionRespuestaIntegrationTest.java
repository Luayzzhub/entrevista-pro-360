package pe.utp.entrevistapro360.presentation;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.util.LinkedMultiValueMap;
import software.amazon.awssdk.services.s3.S3Client;
import pe.utp.entrevistapro360.support.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(ConfirmacionRespuestaIntegrationTest.S3TestConfig.class)
class ConfirmacionRespuestaIntegrationTest {
    static final S3HttpFixture S3 = new S3HttpFixture();
    @TestConfiguration static class S3TestConfig {
        @Bean @Primary S3Client clienteS3Local() { return S3.cliente(); }
    }
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r) { r.add("integraciones.s3-bucket", () -> "pruebas"); }
    @Autowired MockMvc mvc;
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    record Cuenta(long id, String token) { }
    final List<Long> usuarios = new ArrayList<>();
    Cuenta cuenta; long sesion; int pregunta, segunda;
    @BeforeEach void preparar() throws Exception {
        S3.limpiar(); cuenta = cuenta();
        int puesto = jdbc.queryForObject("SELECT MIN(id_puesto) FROM puestos WHERE activo=TRUE", Integer.class);
        pregunta = jdbc.queryForObject("SELECT id_pregunta FROM preguntas WHERE id_puesto=? AND orden=1", Integer.class, puesto);
        segunda = jdbc.queryForObject("SELECT id_pregunta FROM preguntas WHERE id_puesto=? AND orden=2", Integer.class, puesto);
        var response = mvc.perform(post("/api/v1/sesiones").header("Authorization","Bearer "+cuenta.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("id_puesto",puesto)))).andExpect(status().isCreated()).andReturn().getResponse();
        sesion = json.readTree(response.getContentAsString()).get("id_sesion").asLong();
    }
    @AfterEach void limpiar() {
        S3.noDisponible=false; S3.despuesDePut=()->{};
        for(long id:usuarios) {
            jdbc.update("DELETE FROM respuestas_video WHERE id_sesion IN (SELECT id_sesion FROM sesiones_entrevista WHERE id_usuario=?)",id);
            jdbc.update("DELETE FROM sesiones_entrevista WHERE id_usuario=?",id); jdbc.update("DELETE FROM usuarios WHERE id_usuario=?",id);
        }
    }
    @AfterAll static void cerrar() { S3.close(); }
    private Cuenta cuenta() throws Exception {
        String email="confirmacion-"+UUID.randomUUID()+"@example.test", password="prueba-confirmacion-2026";
        var registro=mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre_completo","Prueba confirmación","email",email,"password",password))))
                .andExpect(status().isCreated()).andReturn().getResponse();
        long id=json.readTree(registro.getContentAsString()).get("id_usuario").asLong(); usuarios.add(id);
        var login=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",email,"password",password)))).andExpect(status().isOk()).andReturn().getResponse();
        return new Cuenta(id,json.readTree(login.getContentAsString()).get("access_token").asText());
    }
    private ResultActions subir(String tipo) throws Exception { return subir(cuenta,sesion,pregunta,tipo); }
    private ResultActions subir(Cuenta c,long id,int p,String tipo) throws Exception {
        Path archivo=MediaFixtures.archivo(tipo);
        return mvc.perform(multipart(HttpMethod.PUT,"/api/v1/sesiones/{id}/preguntas/{p}/respuesta",id,p)
                .file(new MockMultipartFile("video","nombre-no-confiable.webm","text/plain",Files.readAllBytes(archivo)))
                .header("Authorization","Bearer "+c.token()));
    }
    private JsonNode confirmada() throws Exception {
        var r=subir("webm").andExpect(status().isAccepted()).andReturn().getResponse(); var node=json.readTree(r.getContentAsString());
        assertThat(r.getHeader("Location")).isEqualTo("/api/v1/respuestas/"+node.get("id_respuesta").asLong()); return node;
    }
    private void sinRespuesta() throws Exception {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM respuestas_video WHERE id_sesion=?",Integer.class,sesion)).isZero();
        mvc.perform(get("/api/v1/sesiones/{id}",sesion).header("Authorization","Bearer "+cuenta.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado_sesion").value("INICIADA"))
                .andExpect(jsonPath("$.pregunta_habilitada.orden").value(1));
    }
    @Test void confirmaVideoRealProcExactoYConsultaPropiaSinCamposInternos() throws Exception {
        var r=confirmada(); long id=r.get("id_respuesta").asLong();
        assertThat(r.get("estado_procesamiento").asText()).isEqualTo("SUBIDO"); assertThat(r.get("estado_sesion").asText()).isEqualTo("EN_PROCESO");
        assertThat(r.get("etapa_actual").asText()).isEqualTo("EXTRAER_AUDIO"); assertThat(r.get("fecha_grabacion").asText()).endsWith("Z");
        var contexto=json.readTree(jdbc.queryForObject("SELECT procesamiento_contexto FROM respuestas_video WHERE id_respuesta=?",String.class,id));
        if(contexto.isTextual()) contexto=json.readTree(contexto.asText());
        assertThat(contexto.size()).isEqualTo(13); assertThat(contexto.get("version_contexto").asText()).isEqualTo("PROC-1.0");
        assertThat(contexto.get("intentos_por_etapa").get("EVALUAR").get("ciclo_1").asInt()).isZero();
        var estado=mvc.perform(get("/api/v1/respuestas/{id}",id).header("Authorization","Bearer "+cuenta.token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.puede_reintentar").value(false)).andExpect(jsonPath("$.error").doesNotExist()).andReturn().getResponse().getContentAsString();
        assertThat(estado).doesNotContain("public_id_cloud","referencia_audio","bloqueo_hasta_utc","version_ejecucion","modelo_stt");
        mvc.perform(get("/api/v1/sesiones/{id}",sesion).header("Authorization","Bearer "+cuenta.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pregunta_habilitada").doesNotExist()).andExpect(jsonPath("$.respuestas_confirmadas").value(1));
        assertThat(S3.objetos).hasSize(1);
    }
    @Test void reenvioIdempotenteNoReinspeccionaNiLlamaS3InclusoDespuesDeCancelar() throws Exception {
        var primera=confirmada(); int llamadas=S3.peticiones.size(); S3.noDisponible=true;
        var repetida=json.readTree(subir("falso").andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString());
        assertThat(repetida.get("id_respuesta")).isEqualTo(primera.get("id_respuesta"));
        mvc.perform(post("/api/v1/sesiones/{id}/cancelacion",sesion).header("Authorization","Bearer "+cuenta.token())).andExpect(status().isOk());
        subir("falso").andExpect(status().isAccepted()).andExpect(jsonPath("$.id_respuesta").value(primera.get("id_respuesta").asLong()));
        assertThat(S3.peticiones).hasSize(llamadas); assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM respuestas_video WHERE id_sesion=?",Integer.class,sesion)).isEqualTo(1);
    }
    @Test void faltaVideoVacioYTamanosLimiteSon400O413SinPersistir() throws Exception {
        String ruta="/api/v1/sesiones/"+sesion+"/preguntas/"+pregunta+"/respuesta";
        mvc.perform(multipart(HttpMethod.PUT,ruta).header("Authorization","Bearer "+cuenta.token())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores_campo.video").exists());
        mvc.perform(multipart(HttpMethod.PUT,ruta).file(new MockMultipartFile("video",new byte[0])).header("Authorization","Bearer "+cuenta.token())).andExpect(status().isBadRequest());
        var grande=new MockMultipartFile("video",new byte[]{1}) { @Override public long getSize() { return 50_000_000; } };
        mvc.perform(multipart(HttpMethod.PUT,ruta).file(grande).header("Authorization","Bearer "+cuenta.token())).andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.codigo").value("ARCHIVO_DEMASIADO_GRANDE"));
        sinRespuesta(); assertThat(S3.objetos).isEmpty();
    }
    @Test void falsoYCodecInvalidoSon415SinPersistir() throws Exception {
        for(String tipo:List.of("falso","codec-invalido")) subir(tipo).andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.codigo").value("FORMATO_NO_ADMITIDO"));
        sinRespuesta(); assertThat(S3.objetos).isEmpty();
    }
    @Test void faltaAudioYDuracionesFueraDeRangoSon422SinPersistir() throws Exception {
        for(String tipo:List.of("sin-audio","corto","largo")) subir(tipo).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("ARCHIVO_NO_UTILIZABLE"));
        sinRespuesta(); assertThat(S3.objetos).isEmpty();
    }
    @Test void falloDeS3Es503YConservaPreguntaHabilitada() throws Exception {
        S3.noDisponible=true; subir("webm").andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value("ALMACENAMIENTO_NO_DISPONIBLE"));
        sinRespuesta(); assertThat(S3.objetos).isEmpty();
    }
    @Test void ordenPropiedadPreguntaYEstadosTerminalesSeVerificanAntesDeSubir() throws Exception {
        subir(cuenta,sesion,segunda,"webm").andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("PREGUNTA_NO_HABILITADA"));
        subir(cuenta,sesion,Integer.MAX_VALUE,"webm").andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("PREGUNTA_NO_ENCONTRADA"));
        Cuenta otra=cuenta(); subir(otra,sesion,pregunta,"webm").andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("SESION_NO_ENCONTRADA"));
        for(String estado:List.of("CANCELADA","COMPLETADA")) {
            jdbc.update("UPDATE sesiones_entrevista SET estado_sesion=?,fecha_fin=?,puntaje_promedio=? WHERE id_sesion=?",estado,LocalDateTime.now(Clock.systemUTC()).plusSeconds(1).withNano(0),estado.equals("COMPLETADA")?80:null,sesion);
            subir("webm").andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("SESION_NO_ADMITE_RESPUESTAS"));
        }
        assertThat(S3.peticiones).isEmpty();
    }
    @Test void consultaAjenaInexistenteIdsYAutenticacionConservanContrato() throws Exception {
        var r=confirmada(); long id=r.get("id_respuesta").asLong(); Cuenta otra=cuenta();
        for(long buscada:List.of(id,Long.MAX_VALUE)) mvc.perform(get("/api/v1/respuestas/{id}",buscada).header("Authorization","Bearer "+otra.token())).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("RESPUESTA_NO_ENCONTRADA"));
        for(String invalido:List.of("0","-1","abc")) mvc.perform(get("/api/v1/respuestas/{id}",invalido).header("Authorization","Bearer "+cuenta.token())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/respuestas/{id}",id)).andExpect(status().isUnauthorized());
        mvc.perform(multipart(HttpMethod.PUT,"/api/v1/sesiones/{s}/preguntas/{p}/respuesta",sesion,pregunta).file(new MockMultipartFile("video",new byte[]{1}))).andExpect(status().isUnauthorized());
    }
    private ResponseEntity<String> subirHttp() throws Exception {
        var datos=new LinkedMultiValueMap<String,Object>(); datos.add("video",new FileSystemResource(MediaFixtures.archivo("webm")));
        HttpHeaders headers=new HttpHeaders(); headers.setBearerAuth(cuenta.token()); headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return http.exchange("/api/v1/sesiones/"+sesion+"/preguntas/"+pregunta+"/respuesta",HttpMethod.PUT,new HttpEntity<>(datos,headers),String.class);
    }
    @Test void dosPutHttpConcurrentesCompartenIdFilaYObjetoSinSobrescritura() throws Exception {
        var pool=Executors.newFixedThreadPool(2); CountDownLatch subido=new CountDownLatch(1), liberar=new CountDownLatch(1);
        S3.despuesDePut=()->{subido.countDown();try{liberar.await(8,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}};
        try {
            var a=pool.submit(this::subirHttp); assertThat(subido.await(8,TimeUnit.SECONDS)).isTrue();
            var b=pool.submit(this::subirHttp); var segunda=b.get(8,TimeUnit.SECONDS); liberar.countDown(); var primera=a.get(8,TimeUnit.SECONDS);
            assertThat(primera.getStatusCode().value()).isEqualTo(202); assertThat(segunda.getStatusCode().value()).isEqualTo(202);
            assertThat(json.readTree(primera.getBody()).get("id_respuesta")).isEqualTo(json.readTree(segunda.getBody()).get("id_respuesta"));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM respuestas_video WHERE id_sesion=?",Integer.class,sesion)).isEqualTo(1);
            assertThat(S3.objetos).hasSize(1); assertThat(S3.peticiones.stream().noneMatch(p->p.metodo().equals("DELETE"))).isTrue();
        } finally {liberar.countDown();pool.shutdownNow();}
    }
    @Test void cancelarMientrasSubeNoRetieneLockYCompensaObjetoNuevo() throws Exception {
        var pool=Executors.newFixedThreadPool(2); CountDownLatch subido=new CountDownLatch(1), liberar=new CountDownLatch(1);
        S3.despuesDePut=()->{subido.countDown();try{liberar.await(8,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}};
        try {
            var subida=pool.submit(this::subirHttp); assertThat(subido.await(8,TimeUnit.SECONDS)).isTrue();
            var cancelacion=pool.submit(()->mvc.perform(post("/api/v1/sesiones/{id}/cancelacion",sesion).header("Authorization","Bearer "+cuenta.token())).andReturn().getResponse().getStatus());
            assertThat(cancelacion.get(3,TimeUnit.SECONDS)).isEqualTo(200); liberar.countDown();
            var respuesta=subida.get(8,TimeUnit.SECONDS); assertThat(respuesta.getStatusCode().value()).isEqualTo(409);
            assertThat(json.readTree(respuesta.getBody()).get("codigo").asText()).isEqualTo("SESION_NO_ADMITE_RESPUESTAS");
            assertThat(S3.objetos).isEmpty(); assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM respuestas_video WHERE id_sesion=?",Integer.class,sesion)).isZero();
        } finally {liberar.countDown();pool.shutdownNow();}
    }
    public static class RechazarInsert implements org.h2.api.Trigger { @Override public void fire(Connection c,Object[] oldRow,Object[] newRow) throws SQLException {throw new SQLException("Fallo de inserción controlado para verificar compensación");} }
    @Test void falloRealDeInsercionRevierteSesionYEliminaObjetoRecienCreado() throws Exception {
        boolean mysql=jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>) c -> c.getMetaData().getDatabaseProductName().equalsIgnoreCase("MySQL"));
        String trigger="ep360_fallo_confirmacion";
        if(mysql) jdbc.execute("CREATE TRIGGER "+trigger+" BEFORE INSERT ON respuestas_video FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Fallo controlado de prueba'");
        else jdbc.execute("CREATE TRIGGER "+trigger+" BEFORE INSERT ON respuestas_video FOR EACH ROW CALL '"+RechazarInsert.class.getName()+"'");
        try {
            subir("webm").andExpect(status().isInternalServerError()); sinRespuesta(); assertThat(S3.objetos).isEmpty();
            assertThat(S3.peticiones.stream().anyMatch(p->p.metodo().equals("DELETE"))).isTrue();
        } finally {jdbc.execute("DROP TRIGGER "+trigger);}
    }
}
