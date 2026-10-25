package pe.utp.entrevistapro360.infrastructure.storage;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import pe.utp.entrevistapro360.application.*;
import pe.utp.entrevistapro360.application.port.out.AlmacenamientoVideoPort;
@Component
public class S3AlmacenamientoVideoAdapter implements AlmacenamientoVideoPort {
    private static final Logger LOG = LoggerFactory.getLogger(S3AlmacenamientoVideoAdapter.class);
    private final S3Client s3;
    private final String bucket;
    public S3AlmacenamientoVideoAdapter(S3Client s3, @Value("${integraciones.s3-bucket:}") String bucket) { this.s3 = s3; this.bucket = bucket; }
    @Override public ResultadoAlmacenamiento asegurarObjetoPrivado(String clave, Path archivo, long bytes, String contentType) {
        if (bucket.isBlank()) throw new AlmacenamientoNoDisponibleException();
        try {
            String huella = sha256(archivo);
            try {
                s3.putObject(PutObjectRequest.builder().bucket(bucket).key(clave).contentType(contentType).contentLength(bytes)
                        .ifNoneMatch("*").metadata(Map.of("sha256", huella)).build(), RequestBody.fromFile(archivo));
                return new ResultadoAlmacenamiento(clave, true);
            } catch (S3Exception ex) {
                if (ex.statusCode() != 412 && !(ex.awsErrorDetails() != null && "ConditionalRequestConflict".equals(ex.awsErrorDetails().errorCode()))) throw ex;
                var head = s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(clave).build());
                // Nunca asociar a la BD metadatos de una toma distinta al objeto que ganó la escritura condicional.
                if (head.contentLength() != bytes || (head.metadata().containsKey("sha256") && !huella.equals(head.metadata().get("sha256"))))
                    throw new AlmacenamientoNoDisponibleException();
                return new ResultadoAlmacenamiento(clave, false);
            }
        } catch (RuntimeException | IOException | NoSuchAlgorithmException ex) { throw new AlmacenamientoNoDisponibleException(); }
    }
    @Override public void verificarObjetoPrivado(String clave) {
        if (bucket.isBlank()) throw new AlmacenamientoNoDisponibleException();
        try { s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(clave).build()); }
        catch (RuntimeException ex) { throw new AlmacenamientoNoDisponibleException(); }
    }
    @Override public void eliminarSilenciosamente(String clave) {
        if (bucket.isBlank()) return;
        try { s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(clave).build()); }
        catch (RuntimeException ex) { LOG.warn("No se pudo completar la compensación del video privado."); }
    }
    private String sha256(Path archivo) throws IOException, NoSuchAlgorithmException {
        var digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(archivo)) { byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) != -1) digest.update(buf, 0, n); }
        return HexFormat.of().formatHex(digest.digest());
    }
}
