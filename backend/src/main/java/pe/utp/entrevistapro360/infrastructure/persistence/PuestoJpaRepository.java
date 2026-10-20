package pe.utp.entrevistapro360.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuestoJpaRepository extends JpaRepository<PuestoJpaEntity, Integer> {
    List<PuestoJpaEntity> findByActivoTrueOrderByIdAsc();
    Optional<PuestoJpaEntity> findByIdAndActivoTrue(Integer id);
}
