package pe.utp.entrevistapro360.infrastructure;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

/** Solo se ejecuta con TEST_DB_URL y el perfil mysql; nunca utiliza bases personales. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = "jdbc:mysql:.*")
class MySqlSchemaIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    private static final Map<String, Integer> COLUMNAS = Map.of(
            "usuarios", 9, "puestos", 6, "preguntas", 8, "sesiones_entrevista", 7,
            "respuestas_video", 10, "transcripciones", 6, "metricas_comunicacion", 8, "evaluaciones_star", 14);

    @Test void ochoTablasSesentaYOchoColumnasEngineYCollationExactos() {
        var tablas = jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name <> 'flyway_schema_history'", String.class);
        assertThat(tablas).containsExactlyInAnyOrderElementsOf(COLUMNAS.keySet());
        int total = 0;
        for (var tabla : COLUMNAS.entrySet()) {
            int columnas = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=?", Integer.class, tabla.getKey());
            assertThat(columnas).as(tabla.getKey()).isEqualTo(tabla.getValue());
            total += columnas;
            assertThat(jdbc.queryForObject("SELECT engine FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?", String.class, tabla.getKey())).isEqualTo("InnoDB");
            assertThat(jdbc.queryForObject("SELECT table_collation FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?", String.class, tabla.getKey())).isEqualTo("utf8mb4_unicode_ci");
        }
        assertThat(total).isEqualTo(68);
    }

    @Test void tiposJsonYAccionesReferencialesSeConservanEnMySql() {
        assertThat(jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND data_type='json'", String.class))
                .containsExactlyInAnyOrder("procesamiento_contexto", "detalle_muletillas", "detalle_star", "areas_mejora");
        var reglas = jdbc.queryForList("SELECT delete_rule FROM information_schema.referential_constraints WHERE constraint_schema=DATABASE()", String.class);
        assertThat(reglas).hasSize(8);
        assertThat(Collections.frequency(reglas, "CASCADE")).isEqualTo(4);
        assertThat(Collections.frequency(reglas, "RESTRICT")).isEqualTo(4);
        assertThat(jdbc.queryForList("SELECT update_rule FROM information_schema.referential_constraints WHERE constraint_schema=DATABASE()", String.class)).allMatch("CASCADE"::equals);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=1", Integer.class)).isEqualTo(2);
    }
}
