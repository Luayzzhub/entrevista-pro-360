package pe.utp.entrevistapro360.infrastructure.media;
import java.nio.file.*;
import javax.sound.sampled.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import pe.utp.entrevistapro360.support.MediaFixtures;
import pe.utp.entrevistapro360.application.FalloEtapaException;
import static org.assertj.core.api.Assertions.*;
class FfmpegExtractorAudioAdapterTest {
    @TempDir Path dir;
    @Test void extraeWebmYMp4RealesPcmMono16kSinRecortar() throws Exception {
        var adapter=new FfmpegExtractorAudioAdapter("ffmpeg");
        for(String tipo:java.util.List.of("webm","mp4")) {
            Path wav=dir.resolve(tipo+".wav"); adapter.extraer(MediaFixtures.archivo(tipo),wav);
            try(var a=AudioSystem.getAudioInputStream(wav.toFile())) {
                assertThat(a.getFormat().getSampleRate()).isEqualTo(16000);
                assertThat(a.getFormat().getChannels()).isEqualTo(1); assertThat(a.getFormat().getSampleSizeInBits()).isEqualTo(16);
                assertThat(a.getFrameLength()/16000.0).isBetween(1.9,2.2);
            }
        }
    }
    @Test void rechazaSinAudioCorruptoYBinarioAusente() throws Exception {
        for(String tipo:java.util.List.of("sin-audio","falso")) {
            Path input=MediaFixtures.archivo(tipo);
            assertThatThrownBy(()->new FfmpegExtractorAudioAdapter("ffmpeg").extraer(input,dir.resolve("out.wav")))
                .isInstanceOfSatisfying(FalloEtapaException.class,e->{assertThat(e.codigo()).isEqualTo("AUDIO_NO_UTILIZABLE");assertThat(e.recuperable()).isFalse();});
        }
        assertThatThrownBy(()->new FfmpegExtractorAudioAdapter(dir.resolve("no-existe.exe").toString()).extraer(dir.resolve("x"),dir.resolve("y")))
                .isInstanceOfSatisfying(FalloEtapaException.class,e->assertThat(e.codigo()).isEqualTo("CONFIGURACION_FFMPEG"));
    }
}

