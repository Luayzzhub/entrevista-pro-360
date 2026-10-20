package pe.utp.entrevistapro360.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transcripcion(Long idTranscripcion, Long idRespuesta, String textoTranscrito,
                            BigDecimal confianzaStt, String idioma, LocalDateTime fechaTranscripcion) { }
