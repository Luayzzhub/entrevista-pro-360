package pe.utp.entrevistapro360.infrastructure.persistence;

import jakarta.persistence.*;
import pe.utp.entrevistapro360.domain.Puesto;

@Entity
@Table(name = "puestos")
public class PuestoJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_puesto")
    private Integer id;
    @Column(name = "nombre_puesto", nullable = false, length = 100, unique = true)
    private String nombre;
    @Column(nullable = false, length = 100) private String area;
    @Column(columnDefinition = "TEXT") private String descripcion;
    @Column(name = "nivel_experiencia", nullable = false, length = 50) private String nivelExperiencia;
    @Column(nullable = false) private boolean activo;

    protected PuestoJpaEntity() { }

    public Puesto toDomain() { return new Puesto(id, nombre, area, descripcion, nivelExperiencia, activo); }
}
