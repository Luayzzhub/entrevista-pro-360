package pe.utp.entrevistapro360.infrastructure.config;

import org.junit.jupiter.api.Test;
import pe.utp.entrevistapro360.infrastructure.security.BCryptPasswordHasher;
import static org.assertj.core.api.Assertions.*;

class IdentidadConfigTest {
    private final IdentidadConfig config = new IdentidadConfig();
    @Test void claveSeValidaEnBytesUtf8InclusoSiHayMenosDeTreintaYDosCaracteres() {
        assertThat(config.jwtSecretKey("á".repeat(16), false).getEncoded()).hasSize(32);
        assertThatThrownBy(() -> config.jwtSecretKey("a".repeat(31), true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.jwtSecretKey("", false)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void claveLocalAusenteGeneraMaterialAleatorioSinConstanteCompartida() {
        byte[] primera = config.jwtSecretKey("", true).getEncoded();
        byte[] segunda = config.jwtSecretKey("", true).getEncoded();
        assertThat(primera).hasSize(32).isNotEqualTo(segunda);
    }
    @Test void bcryptUsaCostoDoceFueraDeLaConfiguracionRapidaDeIntegracion() {
        var hasher = new BCryptPasswordHasher(12);
        String hash = hasher.hash("  contraseña con espacios  ");
        assertThat(hash).startsWith("$2a$12$");
        assertThat(hasher.coincide("  contraseña con espacios  ", hash)).isTrue();
        assertThat(hasher.coincide("contraseña con espacios", hash)).isFalse();
    }
}
