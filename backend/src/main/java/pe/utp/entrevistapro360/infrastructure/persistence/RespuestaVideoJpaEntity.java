package pe.utp.entrevistapro360.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.utp.entrevistapro360.domain.RespuestaVideo;

@Entity
@Table(name = "respuestas_video", uniqueConstraints = @UniqueConstraint(columnNames = {"id_sesion", "id_pregunta"}))
public class RespuestaVideoJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_respuesta") private Long id;
    @Column(name = "id_sesion", nullable = false) private Long sesionId;
    @Column(name = "id_pregunta", nullable = false) private Integer preguntaId;
    @Column(name = "public_id_cloud", nullable = false, length = 150) private String publicIdCloud;
    @Column(name = "duracion_segundos", nullable = false, precision = 6, scale = 3) private BigDecimal duracionSegundos;
    @Column(nullable = false, length = 20) private String formato;
    @Column(name = "tamano_bytes", nullable = false) private Long tamanoBytes;
    @Enumerated(EnumType.STRING) @Column(name = "estado_procesamiento", nullable = false) private RespuestaVideo.EstadoProcesamiento estadoProcesamiento;
    @Column(name = "fecha_grabacion", nullable = false) private LocalDateTime fechaGrabacion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "procesamiento_contexto", nullable = false, columnDefinition = "JSON") private Map<String, Object> procesamientoContexto;
    protected RespuestaVideoJpaEntity() { }
    void actualizarProcesamiento(RespuestaVideo r) { estadoProcesamiento=r.estadoProcesamiento();procesamientoContexto=r.procesamientoContexto(); }
    public static RespuestaVideoJpaEntity nueva(RespuestaVideo r) {
        var e = new RespuestaVideoJpaEntity();
        e.sesionId = r.idSesion(); e.preguntaId = r.idPregunta(); e.publicIdCloud = r.publicIdCloud();
        e.duracionSegundos = r.duracionSegundos(); e.formato = r.formato(); e.tamanoBytes = r.tamanoBytes();
        e.estadoProcesamiento = r.estadoProcesamiento(); e.fechaGrabacion = r.fechaGrabacion(); e.procesamientoContexto = r.procesamientoContexto();
        return e;
    }
    RespuestaVideo toDomain() {
        return new RespuestaVideo(id, sesionId, preguntaId, publicIdCloud, duracionSegundos, formato, tamanoBytes, estadoProcesamiento, fechaGrabacion, procesamientoContexto);
    }
}
