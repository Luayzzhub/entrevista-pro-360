package pe.utp.entrevistapro360.support;
import com.sun.net.httpserver.HttpExchange;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
/** Verificación criptográfica local de GET SigV4; no sustituye políticas IAM de AWS. */
public final class FirmaS3Fixture {
    private FirmaS3Fixture(){}
    public static boolean verificar(HttpExchange x,Clock clock){
        try{
            if(!x.getRequestMethod().equals("GET") || x.getRequestURI().getRawQuery()==null)return false;
            Map<String,String> q=new TreeMap<>();
            for(String par:x.getRequestURI().getRawQuery().split("&")){
                String[] kv=par.split("=",2);if(kv.length!=2)return false;
                String k=URLDecoder.decode(kv[0],StandardCharsets.UTF_8),v=URLDecoder.decode(kv[1],StandardCharsets.UTF_8);
                if(q.put(k,v)!=null)return false;
            }
            if(!"AWS4-HMAC-SHA256".equals(q.get("X-Amz-Algorithm")) || !"300".equals(q.get("X-Amz-Expires")) || !"host".equals(q.get("X-Amz-SignedHeaders")))return false;
            Instant inicio=LocalDateTime.parse(q.get("X-Amz-Date"),DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")).toInstant(ZoneOffset.UTC);
            if(clock.instant().isBefore(inicio) || !clock.instant().isBefore(inicio.plusSeconds(300)))return false;
            String[] cred=q.get("X-Amz-Credential").split("/");
            if(cred.length!=5 || !"ep360-local-test".equals(cred[0]) || !"s3".equals(cred[3]) || !"aws4_request".equals(cred[4]))return false;
            String firma=q.remove("X-Amz-Signature");
            String canonicalQuery=q.entrySet().stream().map(e->encode(e.getKey())+"="+encode(e.getValue())).sorted().collect(java.util.stream.Collectors.joining("&"));
            String canonical="GET\n"+x.getRequestURI().getRawPath()+"\n"+canonicalQuery+"\nhost:"+x.getRequestHeaders().getFirst("Host")+"\n\nhost\nUNSIGNED-PAYLOAD";
            String scope=String.join("/",Arrays.copyOfRange(cred,1,5));
            String mensaje="AWS4-HMAC-SHA256\n"+q.get("X-Amz-Date")+"\n"+scope+"\n"+hex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
            byte[] key=hmac(("AWS4ep360-local-test-secret").getBytes(StandardCharsets.UTF_8),cred[1]);
            key=hmac(key,cred[2]);key=hmac(key,cred[3]);key=hmac(key,cred[4]);
            return MessageDigest.isEqual(hex(hmac(key,mensaje)).getBytes(StandardCharsets.US_ASCII),firma.getBytes(StandardCharsets.US_ASCII));
        }catch(Exception ex){return false;}
    }
    private static byte[] hmac(byte[] key,String value)throws Exception{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));}
    private static String hex(byte[] b){return HexFormat.of().formatHex(b);}
    private static String encode(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8).replace("+","%20").replace("*","%2A").replace("%7E","~");}
}

