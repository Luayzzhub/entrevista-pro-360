package pe.utp.entrevistapro360.domain;
/** Datos persistidos de una respuesta; los bloques ausentes se conservan como null. */
public record ResultadoRespuestaSesion(RespuestaVideo respuesta,Transcripcion transcripcion,MetricaComunicacion metrica,EvaluacionSTAR evaluacion) {
    public boolean esValidoPara(Pregunta pregunta,Long sesion) {
        if(respuesta==null || respuesta.idRespuesta()==null || !java.util.Objects.equals(respuesta.idSesion(),sesion)
                || !respuesta.idPregunta().equals(pregunta.idPregunta()) || respuesta.estadoProcesamiento()!=RespuestaVideo.EstadoProcesamiento.EVALUADO
                || transcripcion==null || !respuesta.idRespuesta().equals(transcripcion.idRespuesta())
                || transcripcion.textoTranscrito()==null || transcripcion.textoTranscrito().isBlank()
                || metrica==null || !respuesta.idRespuesta().equals(metrica.idRespuesta()) || !"MET-1.0".equals(metrica.versionMetricas())
                || metrica.conteoPalabras()<1 || metrica.palabrasPorMinuto()==null || metrica.palabrasPorMinuto().signum()<=0
                || metrica.conteoMuletillas()<0 || metrica.detalleMuletillas()==null
                || evaluacion==null || !respuesta.idRespuesta().equals(evaluacion.idRespuesta())) return false;
        try {
            ValidadorEvaluacion.validarGuardada(evaluacion,pregunta.aplicaStar());return true;
        }catch(SalidaEvaluacionInvalidaException ex){return false;}
    }
}

