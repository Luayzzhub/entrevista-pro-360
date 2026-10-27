package pe.utp.entrevistapro360.infrastructure.storage;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.http.*;
import software.amazon.awssdk.regions.Region;
import pe.utp.entrevistapro360.application.*;
import pe.utp.entrevistapro360.application.port.out.FirmaVideoPort;
@Component
public class FirmaS3VideoAdapter implements FirmaVideoPort {
    private final AwsCredentialsProvider credenciales;
    private final String bucket,region,endpoint;
    public FirmaS3VideoAdapter(AwsCredentialsProvider credenciales,@Value("${integraciones.s3-bucket:}") String bucket,
            @Value("${integraciones.aws-region:us-east-1}") String region,@Value("${integraciones.s3-endpoint-override:}") String endpoint) {
        this.credenciales=credenciales;this.bucket=bucket;this.region=region;this.endpoint=endpoint;
    }
    @Override public String firmar(String clave,Instant inicio) {
        if(bucket.isBlank())throw new AlmacenamientoNoDisponibleException();
        try{
            String path=Arrays.stream(clave.split("/")).map(s->URLEncoder.encode(s,StandardCharsets.UTF_8).replace("+","%20")).collect(Collectors.joining("/"));
            URI uri=URI.create(endpoint.isBlank()?"https://"+bucket+".s3."+region+".amazonaws.com/"+path
                    :endpoint.replaceAll("/+$","")+"/"+bucket+"/"+path);
            if(!java.util.Set.of("http","https").contains(uri.getScheme()))throw new IllegalArgumentException();
            var request=SdkHttpFullRequest.builder().uri(uri).method(SdkHttpMethod.GET).build();
            var parametros=Aws4PresignerParams.builder().awsCredentials(credenciales.resolveCredentials()).signingName("s3").signingRegion(Region.of(region))
                    .signingClockOverride(Clock.fixed(inicio,ZoneOffset.UTC)).expirationTime(inicio.plusSeconds(300)).doubleUrlEncode(false).normalizePath(false).build();
            return AwsS3V4Signer.create().presign(request,parametros).getUri().toString();
        }catch(RuntimeException ex){throw new AlmacenamientoNoDisponibleException();}
    }
}

