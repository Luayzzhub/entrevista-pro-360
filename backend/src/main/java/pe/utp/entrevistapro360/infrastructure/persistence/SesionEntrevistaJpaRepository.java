package pe.utp.entrevistapro360.infrastructure.persistence;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.utp.entrevistapro360.domain.SesionEntrevista.EstadoSesion;

public interface SesionEntrevistaJpaRepository extends JpaRepository<SesionEntrevistaJpaEntity, Long> {
    Optional<SesionEntrevistaJpaEntity> findFirstByUsuarioIdAndEstadoSesionInOrderByFechaInicioDescIdDesc(Long idUsuario, Collection<EstadoSesion> estados);
    Optional<SesionEntrevistaJpaEntity> findByIdAndUsuarioId(Long id, Long usuarioId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SesionEntrevistaJpaEntity s where s.id = :id and s.usuarioId = :usuarioId")
    Optional<SesionEntrevistaJpaEntity> findPropiaForUpdate(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
    List<SesionEntrevistaJpaEntity> findByUsuarioIdOrderByFechaInicioDescIdDesc(Long usuarioId);
}
