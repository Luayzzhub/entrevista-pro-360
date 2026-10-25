package pe.utp.entrevistapro360.infrastructure.persistence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TranscripcionJpaRepository extends JpaRepository<TranscripcionJpaEntity,Long> { Optional<TranscripcionJpaEntity> findByRespuestaId(Long respuesta); }
