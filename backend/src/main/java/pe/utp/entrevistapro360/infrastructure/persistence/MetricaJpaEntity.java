package pe.utp.entrevistapro360.infrastructure.persistence;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.utp.entrevistapro360.domain.MetricaComunicacion;
@Entity @Table(name="metricas_comunicacion")
public class MetricaJpaEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_metrica") private Long id;
    @Column(name="id_respuesta",nullable=false,unique=true) private Long respuestaId;
    @Column(name="conteo_palabras",nullable=false) private int palabras;
    @Column(name="palabras_por_minuto",nullable=false,precision=6,scale=2) private BigDecimal ppm;
    @Column(name="conteo_muletillas",nullable=false) private int muletillas;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="detalle_muletillas",columnDefinition="JSON") private Map<String,Object> detalle;
    @Column(name="version_metricas",nullable=false,length=20) private String version;
    @Column(name="fecha_calculo",nullable=false) private LocalDateTime fecha;
    protected MetricaJpaEntity() { }
    static MetricaJpaEntity nueva(MetricaComunicacion m) { var e=new MetricaJpaEntity();e.respuestaId=m.idRespuesta();e.actualizar(m);return e; }
    void actualizar(MetricaComunicacion m) { palabras=m.conteoPalabras();ppm=m.palabrasPorMinuto();muletillas=m.conteoMuletillas();detalle=m.detalleMuletillas();version=m.versionMetricas();fecha=m.fechaCalculo(); }
    MetricaComunicacion toDomain() { return new MetricaComunicacion(id,respuestaId,palabras,ppm,muletillas,detalle,version,fecha); }
}
