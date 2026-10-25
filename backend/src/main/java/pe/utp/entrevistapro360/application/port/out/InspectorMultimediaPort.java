package pe.utp.entrevistapro360.application.port.out;
import java.nio.file.Path;
import pe.utp.entrevistapro360.application.MetadataMultimedia;
public interface InspectorMultimediaPort { MetadataMultimedia inspeccionar(Path archivo, long tamanoBytes); }
