package pe.utp.entrevistapro360.support;
import com.sun.net.httpserver.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
public class SttHttpFixture implements AutoCloseable {
    public record Peticion(String metodo,String ruta,String autorizacion,String tipo,byte[] cuerpo) { }
    public final List<Peticion> peticiones=new CopyOnWriteArrayList<>();
    public volatile int estado=200;
    public volatile String texto="Eh este código-fuente o sea resuelve problemas";
    public volatile String retryAfter="0";
    public volatile Runnable alRecibir=()->{};
    public volatile int demoraCuerpoMillis;
    private final HttpServer servidor;
    public SttHttpFixture() {
        try {
            servidor=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            servidor.createContext("/",x -> {
                try {
                    peticiones.add(new Peticion(x.getRequestMethod(),x.getRequestURI().getPath(),x.getRequestHeaders().getFirst("Authorization"),x.getRequestHeaders().getFirst("Content-Type"),x.getRequestBody().readAllBytes()));
                    alRecibir.run();
                    byte[] body=new ObjectMapper().writeValueAsBytes(Collections.singletonMap("text",texto));
                    x.getResponseHeaders().set("Content-Type","application/json"); x.getResponseHeaders().set("Retry-After",retryAfter);
                    x.sendResponseHeaders(estado,body.length); x.getResponseBody().flush();
                    if(demoraCuerpoMillis>0) {
                        try { Thread.sleep(demoraCuerpoMillis); }
                        catch(InterruptedException ex) { Thread.currentThread().interrupt(); }
                    }
                    x.getResponseBody().write(body);
                } finally { x.close(); }
            });
            servidor.setExecutor(Executors.newCachedThreadPool(r->{var t=new Thread(r,"stt-local-test");t.setDaemon(true);return t;}));
            servidor.start();
        } catch(IOException ex) { throw new IllegalStateException(ex); }
    }
    public String endpoint() { return "http://127.0.0.1:"+servidor.getAddress().getPort(); }
    public void limpiar() { estado=200;texto="Eh este código-fuente o sea resuelve problemas";retryAfter="0";peticiones.clear();alRecibir=()->{};demoraCuerpoMillis=0; }
    @Override public void close() { servidor.stop(0); }
}

