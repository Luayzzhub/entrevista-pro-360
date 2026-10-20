package pe.utp.entrevistapro360.domain;

import java.time.LocalDateTime;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class UsuarioTest {
    private final LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 12, 0);
    private Usuario usuario(String nombre, String email, String resumen) {
        return new Usuario(3L, email, "hash-no-expuesto", nombre, Usuario.Rol.POSTULANTE, Usuario.Estado.ACTIVO, inicio, null, resumen);
    }
    @Test void normalizaNombreYEmail() {
        var u = usuario("  Ana Pérez  ", "  ANA.PEREZ+UTP@EJEMPLO.COM  ", null);
        assertThat(u.nombreCompleto()).isEqualTo("Ana Pérez");
        assertThat(u.email()).isEqualTo("ana.perez+utp@ejemplo.com");
    }
    @Test void normalizacionEmailNoDependeDelLocaleDeLaMaquina() {
        Locale previo = Locale.getDefault();
        try { Locale.setDefault(Locale.forLanguageTag("tr-TR")); assertThat(Usuario.normalizarEmail("INFO@UTP.COM")).isEqualTo("info@utp.com"); }
        finally { Locale.setDefault(previo); }
    }
    @Test void validaLimitesDelNombre() {
        for (String valor : new String[] {null, "", "   ", "a".repeat(151)}) {
            assertThatThrownBy(() -> usuario(valor, "a@b.com", null)).isInstanceOf(ValidacionUsuarioException.class);
        }
        assertThat(usuario(" x ", "a@b.com", null).nombreCompleto()).isEqualTo("x");
        assertThat(usuario("a".repeat(150), "a@b.com", null).nombreCompleto()).hasSize(150);
    }
    @Test void rechazaCorreosInvalidosYLargos() {
        for (String valor : new String[] {null, "", "sin-arroba", "a@@b.com", "a b@b.com", "a..b@b.com", "a.@b.com", "a@-b.com", "a@b", "a".repeat(145) + "@b.com"}) {
            assertThatThrownBy(() -> usuario("Ana", valor, null)).isInstanceOf(ValidacionUsuarioException.class);
        }
        assertThat(Usuario.normalizarEmail("a".repeat(144) + "@b.com")).hasSize(150);
    }
    @Test void resumenVacioSeNormalizaANullYNoInterpretaHtml() {
        for (String resumen : new String[] {null, "", "   "}) assertThat(usuario("Ana", "a@b.com", resumen).resumenPerfil()).isNull();
        assertThat(usuario("Ana", "a@b.com", "  <b>Estudiante</b>  ").resumenPerfil()).isEqualTo("<b>Estudiante</b>");
        assertThat(usuario("Ana", "a@b.com", "x".repeat(500)).resumenPerfil()).hasSize(500);
        assertThatThrownBy(() -> usuario("Ana", "a@b.com", "x".repeat(501))).isInstanceOf(ValidacionUsuarioException.class);
    }
    @Test void cuentaPuntosDeCodigoEnLugarDeUnidadesUtf16() {
        assertThatThrownBy(() -> Usuario.validarPassword("a".repeat(11))).isInstanceOf(ValidacionUsuarioException.class);
        assertThat(Usuario.validarPassword("a".repeat(12))).hasSize(12);
        assertThatThrownBy(() -> Usuario.validarPassword("😀".repeat(11))).isInstanceOf(ValidacionUsuarioException.class);
        assertThat(Usuario.validarPassword("😀".repeat(12))).isEqualTo("😀".repeat(12));
    }
    @Test void limitaBytesUtf8ParaCaracteresMultibyteYEmojis() {
        for (String password : new String[] {"á".repeat(36), "😀".repeat(18), "a".repeat(72)}) {
            assertThat(Usuario.validarPassword(password)).isEqualTo(password);
        }
        for (String password : new String[] {"á".repeat(37), "😀".repeat(19), "a".repeat(73)}) {
            assertThatThrownBy(() -> Usuario.validarPassword(password)).isInstanceOf(ValidacionUsuarioException.class);
        }
    }
    @Test void passwordConservaEspaciosExterioresSinNormalizacionSilenciosa() {
        String password = "  clave exacta de prueba  ";
        assertThat(Usuario.validarPassword(password)).isSameAs(password);
        assertThat(Usuario.validarPassword(" ".repeat(12))).isEqualTo(" ".repeat(12));
    }
    @Test void registroAsignaValoresExclusivosDelServidor() {
        var nuevo = Usuario.registrar("Ana", "a@b.com", "hash", inicio);
        assertThat(nuevo.idUsuario()).isNull();
        assertThat(nuevo.rol()).isEqualTo(Usuario.Rol.POSTULANTE);
        assertThat(nuevo.estado()).isEqualTo(Usuario.Estado.ACTIVO);
        assertThat(nuevo.fechaRegistro()).isEqualTo(inicio);
        assertThat(nuevo.fechaActualizacion()).isNull();
        assertThat(nuevo.resumenPerfil()).isNull();
    }
    @Test void actualizarPerfilConservaTodosLosDatosDeIdentidad() {
        var original = usuario("Ana", "a@b.com", null);
        var actualizado = original.actualizarPerfil("  Ana Pérez ", " Estudiante ", inicio.plusHours(1));
        assertThat(actualizado.idUsuario()).isEqualTo(original.idUsuario());
        assertThat(actualizado.email()).isEqualTo(original.email());
        assertThat(actualizado.passwordHash()).isEqualTo(original.passwordHash());
        assertThat(actualizado.rol()).isEqualTo(original.rol());
        assertThat(actualizado.estado()).isEqualTo(original.estado());
        assertThat(actualizado.fechaRegistro()).isEqualTo(original.fechaRegistro());
        assertThat(actualizado.fechaActualizacion()).isEqualTo(inicio.plusHours(1));
        assertThat(actualizado.nombreCompleto()).isEqualTo("Ana Pérez");
        assertThat(actualizado.resumenPerfil()).isEqualTo("Estudiante");
        assertThat(original.nombreCompleto()).isEqualTo("Ana");
    }
    @Test void validacionDeRegistroInformaTodosLosCamposInvalidos() {
        assertThatThrownBy(() -> Usuario.validarRegistro(" ", "mal", "corta"))
                .isInstanceOfSatisfying(ValidacionUsuarioException.class,
                        ex -> assertThat(ex.erroresCampo()).containsKeys("nombre_completo", "email", "password"));
    }
}
