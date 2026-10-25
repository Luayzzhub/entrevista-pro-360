package pe.utp.entrevistapro360.infrastructure.persistence;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespuestaVideoJpaRepository extends JpaRepository<RespuestaVideoJpaEntity, Long> {
    @org.springframework.data.jpa.repository.Query("select r.sesionId from RespuestaVideoJpaEntity r where r.id=:id")
    Optional<Long> findSesionId(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from RespuestaVideoJpaEntity r where r.id=:id")
    Optional<RespuestaVideoJpaEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Query("select r from RespuestaVideoJpaEntity r, SesionEntrevistaJpaEntity s where r.sesionId=s.id and s.estadoSesion in ('INICIADA','EN_PROCESO') and r.estadoProcesamiento in ('SUBIDO','AUDIO_EXTRAIDO','TRANSCRITO') order by r.id")
    List<RespuestaVideoJpaEntity> findCandidatas(org.springframework.data.domain.Pageable pagina);
    java.util.Optional<RespuestaVideoJpaEntity> findBySesionIdAndPreguntaId(Long sesionId, Integer preguntaId);
    @org.springframework.data.jpa.repository.Query("select r from RespuestaVideoJpaEntity r, SesionEntrevistaJpaEntity s where r.sesionId=s.id and r.id=:id and s.usuarioId=:usuario")
    java.util.Optional<RespuestaVideoJpaEntity> findPropia(@org.springframework.data.repository.query.Param("usuario") Long usuario, @org.springframework.data.repository.query.Param("id") Long id);
    List<RespuestaVideoJpaEntity> findBySesionIdOrderByIdAsc(Long sesionId);
    List<RespuestaVideoJpaEntity> findBySesionIdInOrderByIdAsc(Collection<Long> idsSesion);
}
