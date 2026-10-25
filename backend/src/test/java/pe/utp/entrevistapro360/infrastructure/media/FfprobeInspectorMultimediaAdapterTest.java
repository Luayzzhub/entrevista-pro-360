package pe.utp.entrevistapro360.infrastructure.media;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import pe.utp.entrevistapro360.application.*;
import pe.utp.entrevistapro360.support.MediaFixtures;
import static org.assertj.core.api.Assertions.*;
class FfprobeInspectorMultimediaAdapterTest {
    private final FfprobeInspectorMultimediaAdapter inspector = new FfprobeInspectorMultimediaAdapter(System.getenv().getOrDefault("FFPROBE_BIN", "ffprobe"), new ObjectMapper());
    @Test void aceptaWebmRealVp8Opus() throws Exception { var p = MediaFixtures.archivo("webm"); var m = inspector.inspeccionar(p, Files.size(p)); assertThat(m.contenedor()).isEqualTo("webm"); assertThat(m.codecVideo()).isEqualTo("vp8"); assertThat(m.codecAudio()).isEqualTo("opus"); assertThat(m.duracionSegundos()).isBetween(new java.math.BigDecimal("1.9"),new java.math.BigDecimal("2.1")); }
    @Test void aceptaMp4RealH264Aac() throws Exception { var p = MediaFixtures.archivo("mp4"); var m = inspector.inspeccionar(p, Files.size(p)); assertThat(m.contenedor()).isEqualTo("mp4"); assertThat(m.codecVideo()).isEqualTo("h264"); assertThat(m.codecAudio()).isEqualTo("aac"); }
    @Test void aceptaWebmVp9Opus() throws Exception { var p = MediaFixtures.archivo("vp9"); assertThat(inspector.inspeccionar(p, Files.size(p)).codecVideo()).isEqualTo("vp9"); }
    @Test void webmStreamingSinDuracionDeCabeceraUsaPaquetesReales() throws Exception { var p = MediaFixtures.archivo("streaming"); assertThat(inspector.inspeccionar(p, Files.size(p)).duracionSegundos()).isBetween(new java.math.BigDecimal("1.9"),new java.math.BigDecimal("2.1")); }
    @Test void sinAudioEs422() throws Exception { var p = MediaFixtures.archivo("sin-audio"); assertThatThrownBy(() -> inspector.inspeccionar(p, Files.size(p))).isInstanceOf(ArchivoMultimediaNoUtilizableException.class); }
    @Test void duracionesRealesFueraDelRangoSon422() throws Exception { for (String tipo : java.util.List.of("corto","largo")) { var p = MediaFixtures.archivo(tipo); assertThatThrownBy(() -> inspector.inspeccionar(p, Files.size(p))).isInstanceOf(ArchivoMultimediaNoUtilizableException.class); } }
    @Test void codecNoPermitidoYTextoDisfrazadoSon415() throws Exception { for (String tipo : java.util.List.of("codec-invalido","falso")) { var p = MediaFixtures.archivo(tipo); assertThatThrownBy(() -> inspector.inspeccionar(p, Files.size(p))).isInstanceOf(FormatoMultimediaNoAdmitidoException.class); } }
    @Test void tamanosLimiteYBinarioAusenteTienenErroresTipados() throws Exception {
        assertThatThrownBy(() -> inspector.inspeccionar(Path.of("no-se-ejecuta"),50_000_000)).isInstanceOf(ArchivoDemasiadoGrandeException.class);
        var p = MediaFixtures.archivo("webm");
        assertThatThrownBy(() -> new FfprobeInspectorMultimediaAdapter("binario-que-no-existe-ep360", new ObjectMapper()).inspeccionar(p,Files.size(p))).isInstanceOf(InspectorMultimediaNoDisponibleException.class);
    }
}
