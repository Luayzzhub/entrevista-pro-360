package pe.utp.entrevistapro360.application;
import pe.utp.entrevistapro360.domain.*;
public record TrabajoReclamado(SesionEntrevista sesion, RespuestaVideo respuesta) {
    public long version() { return new ContextoProcesamiento(respuesta).version(); }
}
