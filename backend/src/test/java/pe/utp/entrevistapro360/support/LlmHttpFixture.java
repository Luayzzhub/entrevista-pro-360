package pe.utp.entrevistapro360.support;
import com.sun.net.httpserver.*;
import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
public class LlmHttpFixture implements AutoCloseable {
    public record Peticion(String metodo,String ruta,String autorizacion,JsonNode payload){}
    public final List<Peticion> peticiones=new CopyOnWriteArrayList<>();
    public volatile int estado=200,demoraCuerpo;
    public volatile String modo="correcto",retryAfter="0";
    public volatile Runnable alRecibir=()->{};
    public volatile String sttEndpoint;
    public volatile java.util.function.Function<Boolean,Map<String,Object>> generarSalida=EvaluacionFixtures::salida;
    private final HttpServer server;
    private final ObjectMapper json=new ObjectMapper();
    public LlmHttpFixture(){
        try{
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/v1/audio/transcriptions",x->{
                try{
                    var request=java.net.http.HttpRequest.newBuilder(URI.create(sttEndpoint+"/v1/audio/transcriptions"))
                            .header("Content-Type",x.getRequestHeaders().getFirst("Content-Type"))
                            .header("Authorization",x.getRequestHeaders().getFirst("Authorization"))
                            .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(x.getRequestBody().readAllBytes())).build();
                    var response=java.net.http.HttpClient.newHttpClient().send(request,java.net.http.HttpResponse.BodyHandlers.ofByteArray());
                    x.getResponseHeaders().set("Content-Type","application/json");x.sendResponseHeaders(response.statusCode(),response.body().length);
                    x.getResponseBody().write(response.body());
                }catch(InterruptedException ex){Thread.currentThread().interrupt();}
                finally{x.close();}
            });
            server.createContext("/",x->{
                try{
                    JsonNode payload=json.readTree(x.getRequestBody());
                    peticiones.add(new Peticion(x.getRequestMethod(),x.getRequestURI().getPath(),x.getRequestHeaders().getFirst("Authorization"),payload));
                    alRecibir.run();
                    boolean aplica=json.readTree(payload.path("input").asText()).path("aplica_star").asBoolean();
                    String salida=modo.equals("json_invalido")?"{":json.writeValueAsString(generarSalida.apply(aplica));
                    if(modo.equals("grande"))salida="x".repeat(2_000_001);
                    Object contenido=modo.equals("refusal")?Map.of("type","refusal","refusal","No puedo evaluar"):Map.of("type","output_text","text",salida);
                    byte[] body=json.writeValueAsBytes(Map.of("status",modo.equals("incompleto")?"incomplete":"completed","output",List.of(Map.of("type","message","content",List.of(contenido)))));
                    x.getResponseHeaders().set("Content-Type","application/json");x.getResponseHeaders().set("Retry-After",retryAfter);
                    x.sendResponseHeaders(estado,body.length);x.getResponseBody().flush();
                    if(demoraCuerpo>0)try{Thread.sleep(demoraCuerpo);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
                    x.getResponseBody().write(body);
                }finally{x.close();}
            });
            server.setExecutor(Executors.newCachedThreadPool(r->{var t=new Thread(r,"llm-http-test");t.setDaemon(true);return t;}));server.start();
        }catch(IOException ex){throw new IllegalStateException(ex);}
    }
    public String endpoint(){return "http://127.0.0.1:"+server.getAddress().getPort();}
    public void limpiar(){estado=200;demoraCuerpo=0;modo="correcto";retryAfter="0";peticiones.clear();alRecibir=()->{};generarSalida=EvaluacionFixtures::salida;}
    public void close(){server.stop(0);}
}

