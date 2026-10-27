package pe.utp.entrevistapro360.infrastructure.persistence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EvaluacionJpaRepository extends JpaRepository<EvaluacionJpaEntity,Long> {
    Optional<EvaluacionJpaEntity> findByRespuestaId(Long respuesta);
}

