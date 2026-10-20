package pe.utp.entrevistapro360.presentation;

import com.fasterxml.jackson.databind.*;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.entrevistapro360.application.EmailYaRegistradoException;
import pe.utp.entrevistapro360.application.port.out.*;
import pe.utp.entrevistapro360.domain.Usuario;
import pe.utp.entrevistapro360.infrastructure.security.JwtTokenProvider;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthPerfilIntegrationTest {
    private static final String PASSWORD = "  Contraseña segura 2026  ";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordHasher passwords;
    @Autowired JwtDecoder decoder;
    @Autowired JwtEncoder encoder;

    private String body(Object value) throws Exception { return json.writeValueAsString(value); }
    private JsonNode registrar(String email, String password) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("nombre_completo", "  Ana Pérez ", "email", email, "password", password))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta);
    }
    private JsonNode registrar() throws Exception { return registrar("  ANA@EJEMPLO.COM  ", PASSWORD); }
    private JsonNode login(String email, String password) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private String token() throws Exception { registrar(); return login("ana@ejemplo.com", PASSWORD).get("access_token").asText(); }

    @Test void registroPublicoNormalizaAsignaRolEstadoFechasUtcYNuncaExponeHash() throws Exception {
        JsonNode respuesta = registrar();
        assertThat(respuesta.size()).isEqualTo(8);
        assertThat(respuesta.get("email").asText()).isEqualTo("ana@ejemplo.com");
        assertThat(respuesta.get("nombre_completo").asText()).isEqualTo("Ana Pérez");
        assertThat(respuesta.get("rol").asText()).isEqualTo("POSTULANTE");
        assertThat(respuesta.get("estado").asText()).isEqualTo("ACTIVO");
        assertThat(respuesta.get("fecha_registro").asText()).endsWith("Z");
        assertThat(respuesta.get("fecha_actualizacion").isNull()).isTrue();
        assertThat(respuesta.get("resumen_perfil").isNull()).isTrue();
        assertThat(respuesta.has("password_hash")).isFalse();
        String hash = jdbc.queryForObject("SELECT password_hash FROM usuarios WHERE email='ana@ejemplo.com'", String.class);
        assertThat(hash).startsWith("$2a$04$").isNotEqualTo(PASSWORD);
        assertThat(passwords.coincide(PASSWORD, hash)).isTrue();
        assertThat(passwords.coincide(PASSWORD.trim(), hash)).isFalse();
    }

    @Test void correoDuplicadoNormalizadoDevuelve409() throws Exception {
        registrar();
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("nombre_completo", "Otra", "email", "ANA@EJEMPLO.COM", "password", PASSWORD))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("EMAIL_YA_REGISTRADO"))
                .andExpect(jsonPath("$.errores_campo.email").isString());
    }

    @Test void colisionRealDeLaClaveUnicaSeTraduceAExcepcionDeNegocio() throws Exception {
        registrar();
        Usuario existente = usuarios.buscarPorEmail("ana@ejemplo.com").orElseThrow();
        assertThatThrownBy(() -> usuarios.crear(Usuario.registrar("Duplicado", existente.email(), existente.passwordHash(), existente.fechaRegistro())))
                .isInstanceOf(EmailYaRegistradoException.class);
    }

    @Test void registroInvalidoDevuelveErroresPorCampoYRechazaAsignacionDePrivilegios() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("nombre_completo", " ", "email", "mal", "password", "corta"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores_campo.nombre_completo").isString())
                .andExpect(jsonPath("$.errores_campo.email").isString()).andExpect(jsonPath("$.errores_campo.password").isString());
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("nombre_completo", "Ana", "email", "a@b.com", "password", PASSWORD, "rol", "ADMIN"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("CAMPOS_NO_PERMITIDOS"));
    }

    @Test void registroPermiteEmojisYRechazaMasDeSetentaYDosBytes() throws Exception {
        registrar("emoji@ejemplo.com", "😀".repeat(18));
        login("emoji@ejemplo.com", "😀".repeat(18));
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("nombre_completo", "Ana", "email", "exceso@ejemplo.com", "password", "😀".repeat(19)))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores_campo.password").isString());
    }

    @Test void loginEmiteHs256ConSubRolYExpiracionExactaDeTreintaMinutos() throws Exception {
        long id = registrar().get("id_usuario").asLong();
        JsonNode respuesta = login(" ANA@EJEMPLO.COM ", PASSWORD);
        assertThat(respuesta.get("expires_in").asLong()).isEqualTo(1800);
        assertThat(respuesta.get("token_type").asText()).isEqualTo("Bearer");
        assertThat(respuesta.get("usuario").has("password_hash")).isFalse();
        Jwt jwt = decoder.decode(respuesta.get("access_token").asText());
        assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
        assertThat(jwt.getSubject()).isEqualTo(Long.toString(id));
        assertThat(jwt.getClaimAsString("rol")).isEqualTo("POSTULANTE");
        assertThat(jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond()).isEqualTo(1800);
    }

    @Test void loginDesconocidoPasswordIncorrectoYCuentaInactivaDanElMismo401() throws Exception {
        registrar();
        var casos = List.of(Map.of("email", "nadie@ejemplo.com", "password", PASSWORD),
                Map.of("email", "ana@ejemplo.com", "password", "incorrecta-pero-larga"));
        String anterior = null;
        for (var caso : casos) {
            String error = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(caso)))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
                    .andReturn().getResponse().getContentAsString();
            if (anterior != null) assertThat(error).isEqualTo(anterior);
            anterior = error;
        }
        jdbc.update("UPDATE usuarios SET estado='INACTIVO' WHERE email='ana@ejemplo.com'"); em.clear();
        String inactivo = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("email", "ana@ejemplo.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(inactivo).isEqualTo(anterior);
    }

    @Test void loginVacioYJsonMalformadoProducen400ConContrato() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores_campo").isMap());
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("JSON_INVALIDO"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":13,\"password\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test void perfilObtieneAlPropietarioExclusivamenteDelToken() throws Exception {
        String token = token();
        registrar("otra@ejemplo.com", PASSWORD);
        mvc.perform(get("/api/v1/perfil").queryParam("id_usuario", "999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("ana@ejemplo.com"))
                .andExpect(jsonPath("$.password_hash").doesNotExist()).andExpect(jsonPath("$.*", hasSize(8)));
    }

    @Test void actualizarPerfilPreservaIdentidadYGuardaTextoPlanoYResumenNulo() throws Exception {
        String token = token();
        Usuario antes = usuarios.buscarPorEmail("ana@ejemplo.com").orElseThrow();
        mvc.perform(patch("/api/v1/perfil").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("nombre_completo", " Ana Actualizada ", "resumen_perfil", " <b>Estudiante</b> "))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre_completo").value("Ana Actualizada"))
                .andExpect(jsonPath("$.resumen_perfil").value("<b>Estudiante</b>"))
                .andExpect(jsonPath("$.fecha_actualizacion", endsWith("Z")));
        em.flush(); em.clear();
        Usuario despues = usuarios.buscarPorId(antes.idUsuario()).orElseThrow();
        assertThat(despues.idUsuario()).isEqualTo(antes.idUsuario());
        assertThat(despues.email()).isEqualTo(antes.email());
        assertThat(despues.passwordHash()).isEqualTo(antes.passwordHash());
        assertThat(despues.rol()).isEqualTo(antes.rol());
        assertThat(despues.estado()).isEqualTo(antes.estado());
        assertThat(despues.fechaRegistro()).isEqualTo(antes.fechaRegistro());
        mvc.perform(patch("/api/v1/perfil").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("nombre_completo", "Ana", "resumen_perfil", "   "))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resumen_perfil").value(nullValue()));
    }

    @Test void perfilRechazaCualquierCampoNoPermitidoYDatosFueraDeLimite() throws Exception {
        String token = token();
        for (String campo : List.of("email", "rol", "estado", "password_hash", "id_usuario", "cualquier_otro")) {
            mvc.perform(patch("/api/v1/perfil").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                    .content(body(Map.of("nombre_completo", "Ana", campo, "intento"))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("CAMPOS_NO_PERMITIDOS"));
        }
        mvc.perform(patch("/api/v1/perfil").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("nombre_completo", "Ana", "resumen_perfil", "x".repeat(501)))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores_campo.resumen_perfil").isString());
    }

    @Test void rechazaAusenciaTokenManipuladoVencidoYRolFalsificado() throws Exception {
        String token = token();
        long id = usuarios.buscarPorEmail("ana@ejemplo.com").orElseThrow().idUsuario();
        String[] partes = token.split("\\.");
        partes[2] = (partes[2].charAt(0) == 'A' ? "B" : "A") + partes[2].substring(1);
        String manipulado = String.join(".", partes);
        Instant expirado = Instant.now().minusSeconds(3600);
        String vencido = firmado(id, "POSTULANTE", expirado);
        String rolIncorrecto = firmado(id, "ADMIN", Instant.now().minusSeconds(1));
        mvc.perform(get("/api/v1/perfil")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        for (String invalido : List.of(manipulado, vencido, rolIncorrecto, "no-es-jwt")) {
            mvc.perform(get("/api/v1/perfil").header("Authorization", "Bearer " + invalido))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
                    .andExpect(jsonPath("$.errores_campo").isMap());
        }
    }

    private String firmado(long id, String rol, Instant iat) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer(JwtTokenProvider.ISSUER).subject(Long.toString(id)).issuedAt(iat).expiresAt(iat.plusSeconds(1800)).claim("rol", rol).build())).getTokenValue();
    }

    @Test void tokenEmitidoSeRechazaSiLaCuentaSeBloqueaCambiaRolOSeElimina() throws Exception {
        String token = token();
        for (String estado : List.of("INACTIVO", "BLOQUEADO")) {
            jdbc.update("UPDATE usuarios SET estado=? WHERE email='ana@ejemplo.com'", estado); em.clear();
            mvc.perform(get("/api/v1/perfil").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
        jdbc.update("UPDATE usuarios SET estado='ACTIVO', rol='ADMIN' WHERE email='ana@ejemplo.com'"); em.clear();
        mvc.perform(get("/api/v1/perfil").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        jdbc.update("DELETE FROM usuarios WHERE email='ana@ejemplo.com'"); em.clear();
        mvc.perform(get("/api/v1/perfil").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }
}
