package pe.utp.entrevistapro360.support;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
public class S3HttpFixture implements AutoCloseable {
    public record Objeto(byte[] contenido, String tipo, String sha256) { }
    public record Peticion(String metodo, String clave, String ifNoneMatch, String contentType, String acl, boolean firmada) { }
    public final Map<String, Objeto> objetos = new ConcurrentHashMap<>();
    public final List<Peticion> peticiones = new CopyOnWriteArrayList<>();
    public volatile boolean noDisponible;
    public volatile Runnable despuesDePut = () -> { };
    private final HttpServer server;
    public S3HttpFixture() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::atender);
            server.setExecutor(Executors.newCachedThreadPool(r -> { Thread t = new Thread(r, "s3-local-test"); t.setDaemon(true); return t; }));
            server.start();
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }
    public URI endpoint() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort()); }
    public S3Client cliente() {
        return S3Client.builder().region(Region.US_EAST_1).endpointOverride(endpoint()).forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("ep360-local-test", "ep360-local-test-secret")))
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(3)).apiCallAttemptTimeout(Duration.ofSeconds(2))).build();
    }
    public void limpiar() { objetos.clear(); peticiones.clear(); noDisponible = false; despuesDePut = () -> { }; }
    private void atender(HttpExchange x) throws IOException {
        try {
            String metodo = x.getRequestMethod(), clave = x.getRequestURI().getPath();
            var h = x.getRequestHeaders();
            boolean firmada = h.getFirst("Authorization") != null && h.getFirst("Authorization").startsWith("AWS4-HMAC-SHA256");
            peticiones.add(new Peticion(metodo, clave, h.getFirst("If-None-Match"), h.getFirst("Content-Type"), h.getFirst("x-amz-acl"), firmada));
            if (!firmada) { error(x, 403, "AccessDenied"); return; }
            if (noDisponible) { error(x, 503, "ServiceUnavailable"); return; }
            if (metodo.equals("PUT")) {
                byte[] contenido = x.getRequestBody().readAllBytes();
                if (Optional.ofNullable(h.getFirst("Content-Encoding")).orElse("").contains("aws-chunked")) contenido = desfragmentar(contenido);
                Objeto nuevo = new Objeto(contenido, h.getFirst("Content-Type"), h.getFirst("x-amz-meta-sha256"));
                if (!"*".equals(h.getFirst("If-None-Match"))) { error(x, 400, "InvalidRequest"); return; }
                if (objetos.putIfAbsent(clave, nuevo) != null) { error(x, 412, "PreconditionFailed"); return; }
                despuesDePut.run(); x.getResponseHeaders().set("ETag", "\"objeto-local\""); x.sendResponseHeaders(200, -1);
            } else if (metodo.equals("HEAD")) {
                Objeto o = objetos.get(clave);
                if (o == null) { x.sendResponseHeaders(404, -1); return; }
                x.getResponseHeaders().set("Content-Length", Integer.toString(o.contenido.length));
                x.getResponseHeaders().set("Content-Type", o.tipo);
                if (o.sha256 != null) x.getResponseHeaders().set("x-amz-meta-sha256", o.sha256);
                x.sendResponseHeaders(200, -1);
            } else if (metodo.equals("DELETE")) { objetos.remove(clave); x.sendResponseHeaders(204, -1); }
            else error(x, 403, "AccessDenied");
        } finally { x.close(); }
    }
    private void error(HttpExchange x, int estado, String codigo) throws IOException {
        byte[] body = ("<Error><Code>" + codigo + "</Code><Message>Fallo local controlado</Message></Error>").getBytes(StandardCharsets.UTF_8);
        if (x.getRequestMethod().equals("HEAD")) { x.sendResponseHeaders(estado, -1); return; }
        x.getResponseHeaders().set("Content-Type", "application/xml"); x.sendResponseHeaders(estado, body.length); x.getResponseBody().write(body);
    }
    private byte[] desfragmentar(byte[] raw) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); int inicio = 0;
        while (inicio < raw.length) {
            int fin = inicio; while (fin + 1 < raw.length && !(raw[fin] == 13 && raw[fin + 1] == 10)) fin++;
            String cabecera = new String(raw, inicio, fin - inicio, StandardCharsets.US_ASCII).split(";")[0];
            int cantidad = Integer.parseInt(cabecera, 16); if (cantidad == 0) break;
            inicio = fin + 2; out.write(raw, inicio, cantidad); inicio += cantidad + 2;
        }
        return out.toByteArray();
    }
    @Override public void close() { server.stop(0); }
}
