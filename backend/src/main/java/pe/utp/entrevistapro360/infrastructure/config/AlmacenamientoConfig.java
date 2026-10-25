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
    @Bean(destroyMethod = "close")
    S3Client s3Client(@Value("${integraciones.aws-region:us-east-1}") String region,
                      @Value("${integraciones.s3-endpoint-override:}") String endpoint) {
        var builder = S3Client.builder().region(Region.of(region)).credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(5)).apiCallAttemptTimeout(Duration.ofSeconds(3)));
        if (!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        return builder.build();
    }
}
