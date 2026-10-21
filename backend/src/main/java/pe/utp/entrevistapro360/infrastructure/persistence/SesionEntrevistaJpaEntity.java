package pe.utp.entrevistapro360.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import pe.utp.entrevistapro360.domain.SesionEntrevista;

@Entity
@Table(name = "sesiones_entrevista")
public class SesionEntrevistaJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_sesion") private Long id;
    @Column(name = "id_usuario", nullable = false) private Long usuarioId;
    @Column(name = "id_puesto", nullable = false) private Integer puestoId;
    @Enumerated(EnumType.STRING) @Column(name = "estado_sesion", nullable = false) private SesionEntrevista.EstadoSesion estadoSesion;
    @Column(name = "puntaje_promedio", precision = 5, scale = 2) private BigDecimal puntajePromedio;
    @Column(name = "fecha_inicio", nullable = false) private LocalDateTime fechaInicio;
    @Column(name = "fecha_fin") private LocalDateTime fechaFin;

    protected SesionEntrevistaJpaEntity() { }
    static SesionEntrevistaJpaEntity nueva(SesionEntrevista s) {
        var entity = new SesionEntrevistaJpaEntity();
        entity.usuarioId = s.idUsuario(); entity.puestoId = s.idPuesto(); entity.fechaInicio = s.fechaInicio();
        entity.actualizarEstado(s); return entity;
    }
    void actualizarEstado(SesionEntrevista s) { estadoSesion = s.estadoSesion(); puntajePromedio = s.puntajePromedio(); fechaFin = s.fechaFin(); }
    SesionEntrevista toDomain() { return new SesionEntrevista(id, usuarioId, puestoId, estadoSesion, puntajePromedio, fechaInicio, fechaFin); }
}
