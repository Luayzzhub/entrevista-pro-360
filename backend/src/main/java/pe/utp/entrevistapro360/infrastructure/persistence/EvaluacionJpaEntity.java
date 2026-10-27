package pe.utp.entrevistapro360.infrastructure.persistence;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.utp.entrevistapro360.domain.EvaluacionSTAR;
@Entity @Table(name="evaluaciones_star")
public class EvaluacionJpaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_evaluacion") private Long id;
    @Column(name="id_respuesta",nullable=false,unique=true) private Long respuestaId;
    @Column(name="puntaje_general",precision=5,scale=2,nullable=false) private BigDecimal general;
    @Column(name="puntaje_relevancia",precision=5,scale=2,nullable=false) private BigDecimal relevancia;
    @Column(name="puntaje_claridad",precision=5,scale=2,nullable=false) private BigDecimal claridad;
    @Column(name="puntaje_estructura",precision=5,scale=2,nullable=false) private BigDecimal estructura;
    @Column(name="puntaje_evidencias",precision=5,scale=2,nullable=false) private BigDecimal evidencias;
    @Column(name="aplica_star",nullable=false) private boolean aplica;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="detalle_star",columnDefinition="JSON") private Map<String,Object> detalle;
    @Column(name="version_rubrica",nullable=false,length=20) private String rubrica;
    @Column(name="fortaleza_principal",nullable=false,columnDefinition="TEXT") private String fortaleza;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="areas_mejora",nullable=false,columnDefinition="JSON") private Map<String,Object> areas;
    @Column(nullable=false,columnDefinition="TEXT") private String recomendaciones;
    @Column(name="fecha_evaluacion",nullable=false) private LocalDateTime fecha;
    protected EvaluacionJpaEntity() { }
    static EvaluacionJpaEntity nueva(EvaluacionSTAR e) {
        var n=new EvaluacionJpaEntity();n.respuestaId=e.idRespuesta();n.general=e.puntajeGeneral();n.relevancia=e.puntajeRelevancia();
        n.claridad=e.puntajeClaridad();n.estructura=e.puntajeEstructura();n.evidencias=e.puntajeEvidencias();n.aplica=e.aplicaStar();
        n.detalle=e.detalleStar();n.rubrica=e.versionRubrica();n.fortaleza=e.fortalezaPrincipal();n.areas=e.areasMejora();n.recomendaciones=e.recomendaciones();n.fecha=e.fechaEvaluacion();return n;
    }
    EvaluacionSTAR toDomain() { return new EvaluacionSTAR(id,respuestaId,general,relevancia,claridad,estructura,evidencias,aplica,detalle,rubrica,fortaleza,areas,recomendaciones,fecha); }
}

