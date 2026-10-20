package pe.utp.entrevistapro360.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreguntaJpaRepository extends JpaRepository<PreguntaJpaEntity, Integer> {
    List<PreguntaJpaEntity> findByPuestoIdAndActivoTrueOrderByOrdenAsc(Integer puestoId);
}
