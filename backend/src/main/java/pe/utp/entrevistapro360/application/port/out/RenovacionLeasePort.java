package pe.utp.entrevistapro360.application.port.out;
public interface RenovacionLeasePort {
    interface Vigilancia extends AutoCloseable { @Override void close(); }
    Vigilancia vigilar(Runnable renovar);
}
