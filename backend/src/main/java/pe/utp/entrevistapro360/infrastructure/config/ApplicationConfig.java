package pe.utp.entrevistapro360.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.utp.entrevistapro360.application.ConsultarCatalogoService;
import pe.utp.entrevistapro360.application.port.in.ConsultarCatalogo;
import pe.utp.entrevistapro360.application.port.out.CatalogoRepository;

@Configuration
public class ApplicationConfig {
    @Bean
    ConsultarCatalogo consultarCatalogo(CatalogoRepository repository) {
        return new ConsultarCatalogoService(repository);
    }
}
