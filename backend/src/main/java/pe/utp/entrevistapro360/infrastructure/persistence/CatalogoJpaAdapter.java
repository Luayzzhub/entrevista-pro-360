package pe.utp.entrevistapro360.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.entrevistapro360.application.port.out.CatalogoRepository;
import pe.utp.entrevistapro360.domain.Pregunta;
import pe.utp.entrevistapro360.domain.Puesto;

@Repository
@Transactional(readOnly = true)
public class CatalogoJpaAdapter implements CatalogoRepository {
    private final PuestoJpaRepository puestos;
    private final PreguntaJpaRepository preguntas;

    public CatalogoJpaAdapter(PuestoJpaRepository puestos, PreguntaJpaRepository preguntas) {
        this.puestos = puestos;
        this.preguntas = preguntas;
    }

    @Override public List<Puesto> buscarPuestosActivos() {
        return puestos.findByActivoTrueOrderByIdAsc().stream().map(PuestoJpaEntity::toDomain).toList();
    }
    @Override public Optional<Puesto> buscarPuestoActivo(int idPuesto) {
        return puestos.findByIdAndActivoTrue(idPuesto).map(PuestoJpaEntity::toDomain);
    }
    @Override public Optional<Puesto> buscarPuesto(int idPuesto) {
        return puestos.findById(idPuesto).map(PuestoJpaEntity::toDomain);
    }
    @Override public List<Pregunta> buscarPreguntasActivas(int idPuesto) {
        return preguntas.findByPuestoIdAndActivoTrueOrderByOrdenAsc(idPuesto).stream().map(PreguntaJpaEntity::toDomain).toList();
    }
}
