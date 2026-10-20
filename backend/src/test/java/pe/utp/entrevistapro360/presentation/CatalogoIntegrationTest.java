package pe.utp.entrevistapro360.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Sin mocks: HTTP MVC -> caso de uso -> adaptador JPA -> BD migrada por Flyway. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test void listaSoloLosTresPuestosActivosSinAutenticacion() throws Exception {
        jdbc.update("INSERT INTO puestos(nombre_puesto, area, nivel_experiencia, activo) VALUES ('Oculto','TI','Inicial',FALSE)");
        mvc.perform(get("/api/v1/puestos")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].nombre_puesto", containsInAnyOrder("Practicante de Desarrollo TI", "Soporte Técnico", "Atención al Cliente")))
                .andExpect(jsonPath("$[*].activo", everyItem(is(true))));
    }

    @Test void cadaPuestoTieneSusTresPreguntasEnOrdenConStarSoloEnLaSegunda() throws Exception {
        var nombres = jdbc.queryForList("SELECT nombre_puesto FROM puestos ORDER BY id_puesto", String.class);
        var primeras = java.util.Map.of(
                "Practicante de Desarrollo TI", "Preséntate brevemente y explica qué conocimientos o proyectos de tu formación puedes aportar como practicante de desarrollo.",
                "Soporte Técnico", "Un usuario indica que su computadora no tiene acceso a Internet. ¿Qué comprobaciones iniciales realizarías y en qué orden?",
                "Atención al Cliente", "¿Qué acciones concretas consideras importantes para brindar una buena atención y comprobar que entendiste la necesidad de una persona?");
        for (String nombre : nombres) {
            int id = jdbc.queryForObject("SELECT id_puesto FROM puestos WHERE nombre_puesto = ?", Integer.class, nombre);
            mvc.perform(get("/api/v1/puestos/{id}/preguntas", id))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[*].id_puesto", everyItem(is(id))))
                    .andExpect(jsonPath("$[*].orden", contains(1, 2, 3)))
                    .andExpect(jsonPath("$[*].aplica_star", contains(false, true, false)))
                    .andExpect(jsonPath("$[*].tiempo_limite_seg", everyItem(is(60))))
                    .andExpect(jsonPath("$[0].enunciado", is(primeras.get(nombre))));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM preguntas", Integer.class)).isEqualTo(9);
    }

    @Test void puestoInexistenteDevuelve404YContratoExacto() throws Exception {
        mvc.perform(get("/api/v1/puestos/2147483647/preguntas"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("PUESTO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").isString()).andExpect(jsonPath("$.errores_campo").isMap())
                .andExpect(jsonPath("$.*", hasSize(3)));
    }

    @Test void puestoInactivoDevuelve404AunqueConservePreguntas() throws Exception {
        int id = jdbc.queryForObject("SELECT MIN(id_puesto) FROM puestos", Integer.class);
        jdbc.update("UPDATE puestos SET activo=FALSE WHERE id_puesto=?", id);
        mvc.perform(get("/api/v1/puestos/{id}/preguntas", id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PUESTO_NO_ENCONTRADO"));
    }

    @Test void preguntaInactivaImpideServirUnCatalogoIncompleto() throws Exception {
        int id = jdbc.queryForObject("SELECT MIN(id_puesto) FROM puestos", Integer.class);
        jdbc.update("UPDATE preguntas SET activo=FALSE WHERE id_puesto=? AND orden=2", id);
        mvc.perform(get("/api/v1/puestos/{id}/preguntas", id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CATALOGO_INVALIDO"));
    }

    @Test void rechazaIdsMalformadosNegativosCeroYDesbordados() throws Exception {
        for (String id : java.util.List.of("abc", "-1", "0", "99999999999999999999")) {
            mvc.perform(get("/api/v1/puestos/{id}/preguntas", id)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"))
                    .andExpect(jsonPath("$.errores_campo.id").isString());
        }
    }

    @Test void rutasPrivadasRequierenAutenticacionConErrorJson() throws Exception {
        mvc.perform(get("/api/v1/perfil")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
                .andExpect(jsonPath("$.errores_campo").isMap());
    }

    @Test void rutasPublicasNoImplementadasYMetodosNoPermitidosConservanErrorEstandar() throws Exception {
        mvc.perform(get("/api/v1/auth/no-existe")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.errores_campo").isMap());
        mvc.perform(post("/api/v1/puestos")).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }
}
