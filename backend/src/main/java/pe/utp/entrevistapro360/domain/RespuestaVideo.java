package pe.utp.entrevistapro360.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record RespuestaVideo(Long idRespuesta, Long idSesion, Integer idPregunta,
                            String publicIdCloud, BigDecimal duracionSegundos, String formato,
                            Long tamanoBytes, EstadoProcesamiento estadoProcesamiento,
                            LocalDateTime fechaGrabacion, Map<String, Object> procesamientoContexto) {
    public enum EstadoProcesamiento { SUBIDO, AUDIO_EXTRAIDO, TRANSCRITO, EVALUADO, FALLIDO }
}
