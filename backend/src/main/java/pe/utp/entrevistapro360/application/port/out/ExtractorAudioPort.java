package pe.utp.entrevistapro360.application.port.out;
import java.nio.file.Path;
public interface ExtractorAudioPort { void extraer(Path video, Path wav); }
