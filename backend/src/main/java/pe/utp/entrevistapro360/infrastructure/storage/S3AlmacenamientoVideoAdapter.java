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
    @Override public boolean existePrivado(String clave) {
        configurar();
        try { s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(clave).build()); return true; }
        catch (S3Exception ex) { if (ex.statusCode()==404) return false; throw clasificar(ex); }
        catch (RuntimeException ex) { throw clasificar(ex); }
    }
    @Override public void descargarPrivado(String clave, Path destino) {
        configurar();
        try (var in=s3.getObject(GetObjectRequest.builder().bucket(bucket).key(clave).build());
                var out=Files.newOutputStream(destino)) {
            byte[] buffer=new byte[8192]; long total=0; int n;
            while ((n=in.read(buffer))!=-1) {
                total+=n;
                if (total>52_428_800L) { in.abort(); throw new FalloEtapaException("OBJETO_NO_UTILIZABLE",false); }
                out.write(buffer,0,n);
            }
        } catch (IOException ex) { throw new FalloEtapaException("S3_TRANSPORTE",true); }
        catch (RuntimeException ex) { throw clasificar(ex); }
    }
    @Override public void subirAudioPrivado(String clave, Path wav) {
        configurar();
        try {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(clave).contentType("audio/wav").build(),RequestBody.fromFile(wav));
        } catch (RuntimeException ex) { throw clasificar(ex); }
    }
    private void configurar() { if (bucket.isBlank()) throw new FalloEtapaException("CONFIGURACION_S3",false); }
    private FalloEtapaException clasificar(RuntimeException ex) {
        if (ex instanceof FalloEtapaException fallo) return fallo;
        if (ex instanceof S3Exception s) {
            int status=s.statusCode();
            long retry=0;
            try { retry=Long.parseLong(s.awsErrorDetails().sdkHttpResponse().firstMatchingHeader("Retry-After").orElse("0")); }
            catch (RuntimeException ignored) { }
            return new FalloEtapaException("S3_HTTP_"+status,status==408 || status==429 || status>=500,retry);
        }
        // El proveedor de credenciales falla antes de iniciar cualquier solicitud.
        boolean credenciales=ex.getMessage()!=null && ex.getMessage().toLowerCase(Locale.ROOT).contains("credentials");
        return new FalloEtapaException(credenciales ? "CONFIGURACION_S3" : "S3_TRANSPORTE",!credenciales);
    }
    private String sha256(Path archivo) throws IOException, NoSuchAlgorithmException {
        var digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(archivo)) { byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) != -1) digest.update(buf, 0, n); }
        return HexFormat.of().formatHex(digest.digest());
    }
}
