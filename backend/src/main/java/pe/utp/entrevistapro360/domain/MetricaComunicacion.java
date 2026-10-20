package pe.utp.entrevistapro360.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record MetricaComunicacion(Long idMetrica, Long idRespuesta, int conteoPalabras,
                                BigDecimal palabrasPorMinuto, int conteoMuletillas,
                                Map<String, Object> detalleMuletillas, String versionMetricas,
                                LocalDateTime fechaCalculo) { }
