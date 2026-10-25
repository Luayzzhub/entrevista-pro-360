package pe.utp.entrevistapro360.application.port.out;
import java.nio.file.Path;
public interface TranscripcionSttPort { String transcribir(Path audio, String modelo); }
