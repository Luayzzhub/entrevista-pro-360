package pe.utp.entrevistapro360.infrastructure.config;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
@Configuration
public class AlmacenamientoConfig {
    @Bean(destroyMethod="close") DefaultCredentialsProvider credencialesAws() { return DefaultCredentialsProvider.create(); }
    @Bean(destroyMethod = "close")
    S3Client s3Client(@Value("${integraciones.aws-region:us-east-1}") String region,
                      @Value("${integraciones.s3-endpoint-override:}") String endpoint, software.amazon.awssdk.auth.credentials.AwsCredentialsProvider credenciales) {
        var builder = S3Client.builder().region(Region.of(region)).credentialsProvider(credenciales)
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(60)).apiCallAttemptTimeout(Duration.ofSeconds(60))
                        .retryStrategy(software.amazon.awssdk.retries.StandardRetryStrategy.builder().maxAttempts(1).build()));
        if (!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        return builder.build();
    }
}
