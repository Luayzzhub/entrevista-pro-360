package pe.utp.entrevistapro360.infrastructure.worker;
import java.util.concurrent.*;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.utp.entrevistapro360.application.port.out.RenovacionLeasePort;
@Component
public class RenovacionLeaseAdapter implements RenovacionLeasePort {
    private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r -> {
        Thread hilo=new Thread(r,"ep360-lease"); hilo.setDaemon(true); return hilo;
    });
    private final long intervalo;
    public RenovacionLeaseAdapter(@Value("${integraciones.worker-heartbeat-ms:20000}") long intervalo) {
        if (intervalo<1) throw new IllegalArgumentException("El intervalo debe ser positivo.");
        this.intervalo=intervalo;
    }
    @Override public Vigilancia vigilar(Runnable renovacion) {
        var futuro=executor.scheduleAtFixedRate(() -> {
            try { renovacion.run(); }
            catch (RuntimeException ex) { System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING,"No se pudo renovar el lease."); }
        },intervalo,intervalo,TimeUnit.MILLISECONDS);
        return () -> futuro.cancel(false);
    }
    @PreDestroy public void cerrar() { executor.shutdownNow(); }
}

