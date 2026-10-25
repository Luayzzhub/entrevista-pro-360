package pe.utp.entrevistapro360.infrastructure.config;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import pe.utp.entrevistapro360.application.ProcesarRespuestasService;
import pe.utp.entrevistapro360.application.port.out.*;
@Configuration
public class ProcesamientoConfig {
    @Bean ProcesarRespuestasService procesarRespuestas(ProcesamientoRepository repo, AlmacenamientoVideoPort storage,
            ExtractorAudioPort extractor, TranscripcionSttPort stt, RenovacionLeasePort heartbeat, Clock clock,
            @Value("${integraciones.worker-backoff-1-segundos:5}") long base1,
            @Value("${integraciones.worker-backoff-2-segundos:15}") long base2) {
        if (base1<0 || base2<0) throw new IllegalArgumentException("Las demoras deben ser no negativas.");
        return new ProcesarRespuestasService(repo,storage,extractor,stt,heartbeat,clock,base1,base2);
    }
}

