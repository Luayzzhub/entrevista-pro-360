package pe.utp.entrevistapro360.infrastructure.persistence;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import pe.utp.entrevistapro360.domain.Transcripcion;
@Entity @Table(name="transcripciones")
public class TranscripcionJpaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_transcripcion") private Long id;
    @Column(name="id_respuesta",nullable=false,unique=true) private Long respuestaId;
    @Column(name="texto_transcrito",nullable=false,columnDefinition="MEDIUMTEXT") private String texto;
    @Column(name="confianza_stt",precision=5,scale=4) private BigDecimal confianza;
    @Column(nullable=false,length=10) private String idioma;
    @Column(name="fecha_transcripcion",nullable=false) private LocalDateTime fecha;
    protected TranscripcionJpaEntity() { }
    static TranscripcionJpaEntity nueva(Transcripcion t) { var e=new TranscripcionJpaEntity(); e.respuestaId=t.idRespuesta();e.texto=t.textoTranscrito();e.confianza=null;e.idioma="es";e.fecha=t.fechaTranscripcion();return e; }
    Transcripcion toDomain() { return new Transcripcion(id,respuestaId,texto,confianza,idioma,fecha); }
}
