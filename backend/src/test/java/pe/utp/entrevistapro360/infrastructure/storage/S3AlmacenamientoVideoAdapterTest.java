package pe.utp.entrevistapro360.infrastructure.storage;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import pe.utp.entrevistapro360.application.*;
import pe.utp.entrevistapro360.support.*;
import static org.assertj.core.api.Assertions.*;
class S3AlmacenamientoVideoAdapterTest {
    @Test void putCondicionalPrivadoReconciliacionHeadYDeleteReales() throws Exception {
        try (var server = new S3HttpFixture(); var cliente = server.cliente()) {
            var adapter = new S3AlmacenamientoVideoAdapter(cliente,"pruebas"); var p = MediaFixtures.archivo("webm");
            assertThat(adapter.asegurarObjetoPrivado("usuarios/1/video.webm",p,Files.size(p),"video/webm").creado()).isTrue();
            assertThat(adapter.asegurarObjetoPrivado("usuarios/1/video.webm",p,Files.size(p),"video/webm").creado()).isFalse();
            adapter.verificarObjetoPrivado("usuarios/1/video.webm");
            var put = server.peticiones.get(0);
            assertThat(put.ifNoneMatch()).isEqualTo("*"); assertThat(put.contentType()).isEqualTo("video/webm"); assertThat(put.acl()).isNull(); assertThat(put.firmada()).isTrue();
            assertThat(server.objetos).hasSize(1); assertThat(server.objetos.values().iterator().next().contenido()).isEqualTo(Files.readAllBytes(p));
            assertThat(server.peticiones.stream().filter(r -> r.metodo().equals("HEAD")).count()).isGreaterThanOrEqualTo(1);
            adapter.eliminarSilenciosamente("usuarios/1/video.webm"); assertThat(server.objetos).isEmpty();
        }
    }
    @Test void faltaBucketOFalloHttpSeTraduceA503YSinExcepcionEnDelete() throws Exception {
        try (var server = new S3HttpFixture(); var cliente = server.cliente()) {
            var p = MediaFixtures.archivo("webm");
            assertThatThrownBy(() -> new S3AlmacenamientoVideoAdapter(cliente,"").asegurarObjetoPrivado("clave",p,Files.size(p),"video/webm")).isInstanceOf(AlmacenamientoNoDisponibleException.class);
            server.noDisponible=true; var adapter = new S3AlmacenamientoVideoAdapter(cliente,"pruebas");
            assertThatThrownBy(() -> adapter.asegurarObjetoPrivado("clave",p,Files.size(p),"video/webm")).isInstanceOf(AlmacenamientoNoDisponibleException.class);
            assertThatCode(() -> adapter.eliminarSilenciosamente("clave")).doesNotThrowAnyException();
        }
    }
    @Test void unaTomaDistintaNoSobrescribeNiReutilizaMetadatosIncorrectos() throws Exception {
        try (var server = new S3HttpFixture(); var cliente = server.cliente()) {
            var adapter = new S3AlmacenamientoVideoAdapter(cliente,"pruebas"); var p = MediaFixtures.archivo("webm"); var otro = MediaFixtures.archivo("vp9");
            adapter.asegurarObjetoPrivado("clave",p,Files.size(p),"video/webm");
            assertThatThrownBy(() -> adapter.asegurarObjetoPrivado("clave",otro,Files.size(otro),"video/webm")).isInstanceOf(AlmacenamientoNoDisponibleException.class);
            assertThat(server.objetos.values().iterator().next().contenido()).isEqualTo(Files.readAllBytes(p));
        }
    }
}
