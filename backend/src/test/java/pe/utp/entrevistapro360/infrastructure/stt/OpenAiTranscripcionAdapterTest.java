package pe.utp.entrevistapro360.infrastructure.stt;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import pe.utp.entrevistapro360.support.*;
import pe.utp.entrevistapro360.infrastructure.media.FfmpegExtractorAudioAdapter;
import pe.utp.entrevistapro360.application.FalloEtapaException;
import static org.assertj.core.api.Assertions.*;
class OpenAiTranscripcionAdapterTest {
    @TempDir Path dir;
    SttHttpFixture fixture; Path wav;
    OpenAiTranscripcionAdapter adapter;
    final String modelo="gpt-4o-mini-transcribe-2025-12-15";
    @BeforeEach void preparar() throws Exception {
        fixture=new SttHttpFixture(); wav=dir.resolve("audio.wav");
        new FfmpegExtractorAudioAdapter("ffmpeg").extraer(MediaFixtures.archivo("webm"),wav);
        adapter=new OpenAiTranscripcionAdapter(fixture.endpoint(),"clave-local",new ObjectMapper(),Clock.systemUTC());
    }
    @AfterEach void cerrar() { fixture.close(); }
    @Test void multipartRealExactoSinPiiYRespuestaLimpia() throws Exception {
        fixture.texto="  Hola mundo  "; assertThat(adapter.transcribir(wav,modelo)).isEqualTo("Hola mundo");
        var r=fixture.peticiones.get(0); assertThat(r.metodo()).isEqualTo("POST"); assertThat(r.ruta()).isEqualTo("/v1/audio/transcriptions");
        assertThat(r.autorizacion()).isEqualTo("Bearer clave-local");
        String body=new String(r.cuerpo(),StandardCharsets.ISO_8859_1);
        assertThat(body).contains("name=\"file\"; filename=\"audio.wav\"","Content-Type: audio/wav","name=\"model\"\r\n\r\n"+modelo,"name=\"language\"\r\n\r\nes","name=\"response_format\"\r\n\r\njson");
        assertThat(body).doesNotContain("id_usuario","id_sesion","id_pregunta","nombre_completo","@example","usuarios/");
        assertThat(body.split("Content-Disposition:",-1)).hasSize(5);
        assertThat(r.cuerpo()).containsSubsequence(Files.readAllBytes(wav));
    }
    @Test void erroresTransitorios429503408YRetryAfter() {
        for(int status:List.of(408,429,503)) {
            fixture.estado=status;fixture.retryAfter="31";
            assertThatThrownBy(()->adapter.transcribir(wav,modelo)).isInstanceOfSatisfying(FalloEtapaException.class,e->{
                assertThat(e.recuperable()).isTrue();assertThat(e.retryAfter()).isEqualTo(31);
            });
        }
    }
    @Test void timeoutIncluyeCuerpoAunqueCabecerasYaLlegaron() {
        fixture.demoraCuerpoMillis=2000;
        var corto=new OpenAiTranscripcionAdapter(fixture.endpoint(),"clave-local",new ObjectMapper(),Clock.systemUTC(),Duration.ofMillis(300));
        assertThatThrownBy(()->corto.transcribir(wav,modelo)).isInstanceOfSatisfying(FalloEtapaException.class,e->{
            assertThat(e.recuperable()).isTrue();assertThat(e.codigo()).isIn("STT_TIMEOUT","STT_TRANSPORTE");
        });
    }
    @Test void cuerpoExcesivoNoAcumulaMemoriaSinLimite() {
        fixture.texto="x".repeat(2_000_001);
        assertThatThrownBy(()->adapter.transcribir(wav,modelo)).isInstanceOfSatisfying(FalloEtapaException.class,e->{
            assertThat(e.codigo()).isEqualTo("STT_RESPUESTA_INVALIDA");assertThat(e.recuperable()).isFalse();
        });
    }
    @Test void rechazoDefinitivoCredencialesYTextoVacioNuloBlanco() {
        for(int status:List.of(400,401,403,422)) {
            fixture.estado=status;
            assertThatThrownBy(()->adapter.transcribir(wav,modelo)).isInstanceOfSatisfying(FalloEtapaException.class,e->assertThat(e.recuperable()).isFalse());
        }
        fixture.estado=200;
        for(String texto:Arrays.asList(null,"","   ")) {
            fixture.texto=texto;
            assertThatThrownBy(()->adapter.transcribir(wav,modelo)).isInstanceOfSatisfying(FalloEtapaException.class,e->{
                assertThat(e.codigo()).isEqualTo("TRANSCRIPCION_VACIA");assertThat(e.recuperable()).isFalse();
            });
        }
        assertThatThrownBy(()->new OpenAiTranscripcionAdapter(fixture.endpoint(),"",new ObjectMapper(),Clock.systemUTC()).transcribir(wav,modelo))
                .isInstanceOfSatisfying(FalloEtapaException.class,e->assertThat(e.codigo()).isEqualTo("CONFIGURACION_STT"));
    }
}

