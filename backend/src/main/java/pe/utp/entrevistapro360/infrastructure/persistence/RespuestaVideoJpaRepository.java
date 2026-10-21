package pe.utp.entrevistapro360.infrastructure.persistence;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespuestaVideoJpaRepository extends JpaRepository<RespuestaVideoJpaEntity, Long> {
    List<RespuestaVideoJpaEntity> findBySesionIdOrderByIdAsc(Long sesionId);
    List<RespuestaVideoJpaEntity> findBySesionIdInOrderByIdAsc(Collection<Long> idsSesion);
}
