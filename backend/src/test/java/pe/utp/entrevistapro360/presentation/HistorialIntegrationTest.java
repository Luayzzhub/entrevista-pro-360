package pe.utp.entrevistapro360.presentation;

import com.fasterxml.jackson.databind.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.entrevistapro360.domain.*;
import pe.utp.entrevistapro360.infrastructure.persistence.*;
import pe.utp.entrevistapro360.application.port.out.ProcesamientoRepository;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class HistorialIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc; @Autowired EntityManager em;
    @Autowired RespuestaVideoJpaRepository respuestas; @Autowired ProcesamientoRepository procesamiento;
    record Cuenta(long id,String token) { }
    private Cuenta cuenta() throws Exception {
        String email="historial-"+UUID.randomUUID()+"@example.test", pass="Pruebas-historial-2026";
        var registro=json.readTree(mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre_completo","Historial prueba","email",email,"password",pass))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        var login=json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",email,"password",pass))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        return new Cuenta(registro.get("id_usuario").asLong(),login.get("access_token").asText());
    }
    private int puesto() {return jdbc.queryForObject("SELECT MIN(id_puesto) FROM puestos",Integer.class);}
    private long sesion(Cuenta c,int puesto,String estado,String promedio,String inicio,String fin,int evaluadas,String rubrica) {
        KeyHolder key=new GeneratedKeyHolder();
        jdbc.update(con->{var ps=con.prepareStatement("INSERT INTO sesiones_entrevista(id_usuario,id_puesto,estado_sesion,puntaje_promedio,fecha_inicio,fecha_fin) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1,c.id());ps.setInt(2,puesto);ps.setString(3,estado);ps.setBigDecimal(4,promedio==null?null:new BigDecimal(promedio));
            ps.setObject(5,LocalDateTime.parse(inicio));ps.setObject(6,fin==null?null:LocalDateTime.parse(fin));return ps;},key);
        long id=key.getKey().longValue();
        var preguntas=jdbc.queryForList("SELECT id_pregunta FROM preguntas WHERE id_puesto=? ORDER BY orden",Integer.class,puesto);
        for(int i=0;i<evaluadas;i++) {
            var r=respuestas.saveAndFlush(RespuestaVideoJpaEntity.nueva(new RespuestaVideo(null,id,preguntas.get(i),"fixture-historial",new BigDecimal("30"),"webm",100L,
                    RespuestaVideo.EstadoProcesamiento.EVALUADO,LocalDateTime.parse(inicio),Map.of("fixture",true))));
            procesamiento.guardarEvaluacion(new EvaluacionSTAR(null,(Long) em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(r),new BigDecimal("75.00"),new BigDecimal("75"),new BigDecimal("75"),new BigDecimal("75"),new BigDecimal("75"),
                    false,null,rubrica,"Fortaleza",Map.of(),"Recomendaciones",LocalDateTime.parse(fin==null?inicio:fin)));
        }
        em.flush();em.clear();return id;
    }
    private long completa(Cuenta c,String score,int dia) {return sesion(c,puesto(),"COMPLETADA",score,"2026-10-"+String.format("%02d",dia)+"T10:00:00","2026-10-"+String.format("%02d",dia)+"T11:00:00",3,"COM-1.0");}
    private JsonNode consultar(Cuenta c,String query) throws Exception {return json.readTree(mvc.perform(get("/api/v1/sesiones"+query).header("Authorization","Bearer "+c.token()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
    private String comparable() {return "?id_puesto="+puesto()+"&version_rubrica=COM-1.0";}
    @Test void canonicoSeCalculaAntesDePaginar() throws Exception {
        var c=cuenta();long a=completa(c,"87.50",5),b=completa(c,"75",8),d=completa(c,"79.17",12),e=completa(c,"83.33",16);
        var r=consultar(c,comparable()+"&size=2");assertThat(r.get("items").size()).isEqualTo(2);assertThat(r.get("total_items_filtrados").asInt()).isEqualTo(4);
        assertThat(r.get("total_paginas").asInt()).isEqualTo(2);var comp=r.get("comparacion");assertThat(comp.get("total_sesiones_comparadas").asInt()).isEqualTo(4);
        assertThat(comp.get("promedio_conjunto").decimalValue()).isEqualByComparingTo("81.25");assertThat(comp.get("mejor_puntaje").decimalValue()).isEqualByComparingTo("87.50");
        assertThat(comp.get("cambio_vs_primera").decimalValue()).isEqualByComparingTo("-4.17");assertThat(comp.get("cambio_vs_anterior").decimalValue()).isEqualByComparingTo("4.16");
        assertThat(comp.get("serie").findValues("id_sesion").stream().map(JsonNode::asLong)).containsExactly(a,b,d,e);
        assertThat(consultar(c,comparable()+"&size=2&page=1").get("items").findValues("id_sesion").stream().map(JsonNode::asLong)).containsExactly(b,a);
        assertThat(consultar(c,comparable()+"&size=2&page=2147483647").get("items")).isEmpty();
    }
    @Test void resumenGlobalNoCambiaConFiltrosYListadoDesempataPorId() throws Exception {
        var c=cuenta();long a=completa(c,"75",5),b=completa(c,"80",5);
        long pendiente=sesion(c,puesto(),"INICIADA",null,"2026-10-17T10:00:00",null,0,"COM-1.0");
        var r=consultar(c,"");assertThat(r.get("items").findValues("id_sesion").stream().map(JsonNode::asLong)).containsExactly(pendiente,b,a);
        r=consultar(c,"?estado=CANCELADA");assertThat(r.get("items")).isEmpty();assertThat(r.get("total_sesiones_completadas").asInt()).isEqualTo(2);
        assertThat(r.get("sesion_pendiente").get("id_sesion").asLong()).isEqualTo(pendiente);
        assertThat(r.get("ultima_sesion_completada").get("id_sesion").asLong()).isEqualTo(b);
    }
    @Test void bordeFechaLimaIncluyeHasta235959ConFinExclusivo() throws Exception {
        var c=cuenta();
        long dentro=sesion(c,puesto(),"INICIADA",null,"2026-10-17T04:30:00",null,0,"COM-1.0");
        sesion(c,puesto(),"INICIADA",null,"2026-10-17T05:00:00",null,0,"COM-1.0");
        sesion(c,puesto(),"INICIADA",null,"2026-10-16T04:59:59",null,0,"COM-1.0");
        long borde=sesion(c,puesto(),"INICIADA",null,"2026-10-16T05:00:00",null,0,"COM-1.0");
        var r=consultar(c,"?fecha_desde=2026-10-16&fecha_hasta=2026-10-16");
        assertThat(r.get("items").findValues("id_sesion").stream().map(JsonNode::asLong)).containsExactly(dentro,borde);
    }
    @Test void d10ExcluyeIncompletasCanceladasYVersionesMixtas() throws Exception {
        var c=cuenta();long valida=completa(c,"75",5);
        sesion(c,puesto(),"CANCELADA",null,"2026-10-06T10:00:00","2026-10-06T11:00:00",2,"COM-1.0");
        sesion(c,puesto(),"EN_PROCESO",null,"2026-10-07T10:00:00",null,2,"COM-1.0");
        long mixta=completa(c,"75",8);
        jdbc.update("UPDATE evaluaciones_star SET version_rubrica='OTRA' WHERE id_respuesta=(SELECT MIN(id_respuesta) FROM respuestas_video WHERE id_sesion=?)",mixta);em.clear();
        sesion(c,puesto(),"COMPLETADA","75","2026-10-09T10:00:00","2026-10-09T11:00:00",2,"COM-1.0");
        var r=consultar(c,"?version_rubrica=COM-1.0");assertThat(r.get("items").findValues("id_sesion").stream().map(JsonNode::asLong)).containsExactly(valida);
        assertThat(r.get("aviso_filtro_rubrica").asText()).isEqualTo("El filtro de rúbrica incluye únicamente sesiones completas");
        assertThat(r.get("items").get(0).get("version_rubrica").asText()).isEqualTo("COM-1.0");
        assertThat(consultar(c,"").get("items").get(1).get("version_rubrica").isNull()).isTrue();
    }
    @Test void estadosIncompatiblesIntersectanVacioSinCambiarFiltros() throws Exception {
        var c=cuenta();completa(c,"75",5);
        for(String estado:List.of("INICIADA","EN_PROCESO","CANCELADA")) {
            var r=consultar(c,comparable()+"&estado="+estado);assertThat(r.get("total_items_filtrados").asInt()).isZero();
            assertThat(r.get("items")).isEmpty();assertThat(r.get("comparacion").get("habilitada").asBoolean()).isTrue();
            assertThat(r.get("comparacion").get("promedio_conjunto").isNull()).isTrue();assertThat(r.get("aviso_filtro_rubrica").asText()).contains("únicamente sesiones completas");
        }
    }
    @Test void aislamientoDeUsuarioTablaIndicadoresYSelectores() throws Exception {
        var c=cuenta();completa(c,"75",5);var otro=cuenta();completa(otro,"100",6);
        int otroPuesto=jdbc.queryForObject("SELECT MAX(id_puesto) FROM puestos",Integer.class);
        sesion(otro,otroPuesto,"COMPLETADA","100","2026-10-07T10:00:00","2026-10-07T11:00:00",3,"COM-1.0");
        var r=consultar(c,comparable());assertThat(r.get("items").size()).isEqualTo(1);assertThat(r.get("total_sesiones_completadas").asInt()).isEqualTo(1);
        assertThat(r.get("comparacion").get("serie").size()).isEqualTo(1);assertThat(r.get("comparacion").get("promedio_conjunto").decimalValue()).isEqualByComparingTo("75");
        assertThat(r.get("puestos_historial").size()).isEqualTo(1);
    }
    @Test void ceroYUnaSesionesSinVariacionesInventadas() throws Exception {
        var c=cuenta();var r=consultar(c,comparable()).get("comparacion");
        for(var campo:List.of("promedio_conjunto","mejor_puntaje","cambio_vs_primera","cambio_vs_anterior"))assertThat(r.get(campo).isNull()).isTrue();
        completa(c,"0",5);r=consultar(c,comparable()).get("comparacion");assertThat(r.get("promedio_conjunto").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(r.get("mejor_puntaje").decimalValue()).isEqualByComparingTo("0.00");assertThat(r.get("cambio_vs_primera").isNull()).isTrue();assertThat(r.get("cambio_vs_anterior").isNull()).isTrue();
    }
    @Test void puestoEstadoYRangoRestringenLaSerieCompleta() throws Exception {
        var c=cuenta();completa(c,"80",5);completa(c,"90",16);int otro=jdbc.queryForObject("SELECT MAX(id_puesto) FROM puestos",Integer.class);
        sesion(c,otro,"COMPLETADA","20","2026-10-12T10:00:00","2026-10-12T11:00:00",3,"COM-1.0");
        var r=consultar(c,comparable()+"&fecha_desde=2026-10-10&fecha_hasta=2026-10-20&estado=COMPLETADA");
        assertThat(r.get("total_items_filtrados").asInt()).isEqualTo(1);assertThat(r.get("comparacion").get("promedio_conjunto").decimalValue()).isEqualByComparingTo("90");
        assertThat(r.get("total_sesiones_completadas").asInt()).isEqualTo(3);
    }
    @Test void parametrosInvalidosDevuelven400() throws Exception {
        var c=cuenta();for(String query:List.of("id_puesto=0","id_puesto=-1","id_puesto=no","page=-1","page=no","page=1.5","size=0","size=51","size=no","estado=OTRA","version_rubrica=COM-2.0","fecha_desde=16-10-2026","fecha_hasta=2026-02-30")) {
            mvc.perform(get("/api/v1/sesiones?"+query).header("Authorization","Bearer "+c.token())).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/sesiones?fecha_desde=2026-10-17&fecha_hasta=2026-10-16").header("Authorization","Bearer "+c.token()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.mensaje").value("La fecha inicial no puede ser posterior a la fecha final."))
                .andExpect(jsonPath("$.errores_campo.fecha_desde").isString()).andExpect(jsonPath("$.errores_campo.fecha_hasta").isString());
    }
    @Test void comparacionDeshabilitadaSinAmbosFiltros() throws Exception {
        var c=cuenta();completa(c,"75",5);
        for(String q:List.of("","?id_puesto="+puesto(),"?version_rubrica=COM-1.0")) {
            var r=consultar(c,q).get("comparacion");assertThat(r.get("habilitada").asBoolean()).isFalse();assertThat(r.get("serie")).isEmpty();assertThat(r.get("promedio_conjunto").isNull()).isTrue();
        }
    }
    @Test void puestoHistoricoInactivoSeConservaSinNuevasColumnas() throws Exception {
        var c=cuenta();completa(c,"75",5);jdbc.update("UPDATE puestos SET activo=FALSE WHERE id_puesto=?",puesto());em.clear();
        var r=consultar(c,comparable());assertThat(r.get("puestos_historial").get(0).get("id_puesto").asInt()).isEqualTo(puesto());assertThat(r.get("items").size()).isEqualTo(1);
    }
    @Test void motivosCanceladosSonSegurosYNoSePublicanReferenciasPrivadas() throws Exception {
        var c=cuenta();long s=sesion(c,puesto(),"CANCELADA",null,"2026-10-05T10:00:00","2026-10-05T11:00:00",1,"COM-1.0");
        var q=consultar(c,"").get("items").get(0);assertThat(q.get("motivo_cierre").asText()).contains("anticipadamente");
        assertThat(q.get("puntaje_promedio").isNull()).isTrue();
        var original = procesamiento.buscarResultadosSesion(s).get(0).respuesta();
        procesamiento.actualizar(new RespuestaVideo(original.idRespuesta(), s, original.idPregunta(), original.publicIdCloud(),
                original.duracionSegundos(), original.formato(), original.tamanoBytes(), RespuestaVideo.EstadoProcesamiento.FALLIDO,
                original.fechaGrabacion(), Map.of("ultimo_error", Map.of("recuperable", false, "codigo", "PRIVADO"))));
        em.flush(); em.clear();
        var r=consultar(c,"");assertThat(r.get("items").get(0).get("motivo_cierre").asText()).contains("falla definitiva");
        assertThat(r.toString()).doesNotContain("PRIVADO","public_id_cloud","procesamiento_contexto","referencia_audio");
    }
}
