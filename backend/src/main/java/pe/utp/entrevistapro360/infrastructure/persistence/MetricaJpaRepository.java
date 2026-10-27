package pe.utp.entrevistapro360.infrastructure.persistence;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MetricaJpaRepository extends JpaRepository<MetricaJpaEntity,Long> {
    Optional<MetricaJpaEntity> findByRespuestaId(Long respuesta);
    java.util.List<MetricaJpaEntity> findByRespuestaIdIn(java.util.Collection<Long> respuestas);
}
