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
    @Bean pe.utp.entrevistapro360.application.port.in.GestionarRespuesta gestionarRespuesta(
            SesionRepository sesiones, CatalogoRepository catalogo,
            pe.utp.entrevistapro360.application.port.out.InspectorMultimediaPort inspector,
            pe.utp.entrevistapro360.application.port.out.AlmacenamientoVideoPort almacenamiento, Clock clock,
            @org.springframework.beans.factory.annotation.Value("${integraciones.stt-model}") String stt,
            @org.springframework.beans.factory.annotation.Value("${integraciones.llm-model}") String llm) {
        return new pe.utp.entrevistapro360.application.GestionarRespuestaService(sesiones, catalogo, inspector, almacenamiento, clock, stt, llm);
    }
    @Bean
    ConsultarCatalogo consultarCatalogo(CatalogoRepository repository) {
        return new ConsultarCatalogoService(repository);
    }
    @Bean GestionarSesion gestionarSesion(SesionRepository sesiones, CatalogoRepository catalogo, Clock clock) {
        return new GestionarSesionService(sesiones, catalogo, clock);
    }
}
