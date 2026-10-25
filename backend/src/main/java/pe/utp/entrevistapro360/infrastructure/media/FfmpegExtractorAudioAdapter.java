package pe.utp.entrevistapro360.infrastructure.media;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.utp.entrevistapro360.application.FalloEtapaException;
import pe.utp.entrevistapro360.application.port.out.ExtractorAudioPort;
@Component
public class FfmpegExtractorAudioAdapter implements ExtractorAudioPort {
    private final String binario;
    public FfmpegExtractorAudioAdapter(@Value("${integraciones.ffmpeg-bin:ffmpeg}") String binario) {
        this.binario=BinarioMultimedia.resolver(binario);
    }
    @Override public void extraer(Path video, Path wav) {
        Process proceso;
        try {
            proceso=new ProcessBuilder(binario,"-y","-v","error","-i",video.toString(),"-vn","-acodec","pcm_s16le",
                    "-ar","16000","-ac","1","-f","wav",wav.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        } catch (IOException ex) { throw new FalloEtapaException("CONFIGURACION_FFMPEG",false); }
        try {
            if (!proceso.waitFor(60,TimeUnit.SECONDS)) {
                proceso.destroyForcibly(); proceso.waitFor(5,TimeUnit.SECONDS);
                throw new FalloEtapaException("FFMPEG_TIMEOUT",true);
            }
            if (proceso.exitValue()!=0) throw new FalloEtapaException("AUDIO_NO_UTILIZABLE",false);
            validar(wav);
        } catch (InterruptedException ex) {
            proceso.destroyForcibly(); Thread.currentThread().interrupt();
            throw new FalloEtapaException("FFMPEG_INTERRUMPIDO",true);
        }
    }
    public static void validar(Path wav) {
        try (AudioInputStream audio=AudioSystem.getAudioInputStream(wav.toFile())) {
            var f=audio.getFormat();
            if (!AudioFormat.Encoding.PCM_SIGNED.equals(f.getEncoding()) || f.getSampleSizeInBits()!=16
                    || f.getChannels()!=1 || f.getSampleRate()!=16000 || f.isBigEndian()
                    || Files.size(wav)<=44 || audio.read(new byte[2])!=2)
                throw new FalloEtapaException("AUDIO_NO_UTILIZABLE",false);
        } catch (IOException | UnsupportedAudioFileException ex) { throw new FalloEtapaException("AUDIO_NO_UTILIZABLE",false); }
    }
}

