package pe.utp.entrevistapro360.infrastructure.worker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import pe.utp.entrevistapro360.application.ProcesarRespuestasService;
@Component
@EnableScheduling
@ConditionalOnProperty(name="integraciones.worker-habilitado",havingValue="true",matchIfMissing=true)
public class WorkerRespuestas {
    private final ProcesarRespuestasService servicio;
    public WorkerRespuestas(ProcesarRespuestasService servicio) { this.servicio=servicio; }
    @Scheduled(fixedDelayString="${integraciones.worker-intervalo-ms:2000}")
    public void procesar() {
        try { servicio.procesarSiguiente(); }
        catch (RuntimeException ex) { System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING,"El worker reanudará en el próximo ciclo."); }
    }
}

