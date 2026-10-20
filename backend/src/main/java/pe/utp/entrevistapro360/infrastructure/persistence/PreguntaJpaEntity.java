package pe.utp.entrevistapro360.infrastructure.persistence;

import jakarta.persistence.*;
import pe.utp.entrevistapro360.domain.Pregunta;

@Entity
@Table(name = "preguntas", uniqueConstraints = @UniqueConstraint(columnNames = {"id_puesto", "orden"}))
public class PreguntaJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_pregunta") private Integer id;
    @Column(name = "id_puesto", nullable = false) private Integer puestoId;
    @Column(nullable = false, columnDefinition = "TEXT") private String enunciado;
    @Column(name = "competencia_evaluada", nullable = false, length = 100) private String competenciaEvaluada;
    @Column(nullable = false) private int orden;
    @Column(name = "tiempo_limite_seg", nullable = false) private int tiempoLimiteSeg;
    @Column(nullable = false) private boolean activo;
    @Column(name = "aplica_star", nullable = false) private boolean aplicaStar;

    protected PreguntaJpaEntity() { }

    public Pregunta toDomain() {
        return new Pregunta(id, puestoId, enunciado, competenciaEvaluada, orden, tiempoLimiteSeg, activo, aplicaStar);
    }
}
