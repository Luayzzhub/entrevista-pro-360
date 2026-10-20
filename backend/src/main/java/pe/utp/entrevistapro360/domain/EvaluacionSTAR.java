package pe.utp.entrevistapro360.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record EvaluacionSTAR(Long idEvaluacion, Long idRespuesta, BigDecimal puntajeGeneral,
                            BigDecimal puntajeRelevancia, BigDecimal puntajeClaridad,
                            BigDecimal puntajeEstructura, BigDecimal puntajeEvidencias,
                            boolean aplicaStar, Map<String, Object> detalleStar,
                            String versionRubrica, String fortalezaPrincipal,
                            Map<String, Object> areasMejora, String recomendaciones,
                            LocalDateTime fechaEvaluacion) { }
