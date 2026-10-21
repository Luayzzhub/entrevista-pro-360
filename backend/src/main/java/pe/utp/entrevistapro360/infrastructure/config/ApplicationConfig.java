package pe.utp.entrevistapro360.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.utp.entrevistapro360.application.ConsultarCatalogoService;
import pe.utp.entrevistapro360.application.port.in.ConsultarCatalogo;
import pe.utp.entrevistapro360.application.port.out.CatalogoRepository;
import pe.utp.entrevistapro360.application.GestionarSesionService;
import pe.utp.entrevistapro360.application.port.in.GestionarSesion;
import pe.utp.entrevistapro360.application.port.out.SesionRepository;
import java.time.Clock;

@Configuration
public class ApplicationConfig {
    @Bean
    ConsultarCatalogo consultarCatalogo(CatalogoRepository repository) {
        return new ConsultarCatalogoService(repository);
    }
    @Bean GestionarSesion gestionarSesion(SesionRepository sesiones, CatalogoRepository catalogo, Clock clock) {
        return new GestionarSesionService(sesiones, catalogo, clock);
    }
}
