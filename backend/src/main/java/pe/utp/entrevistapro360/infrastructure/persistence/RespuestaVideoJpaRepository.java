package pe.utp.entrevistapro360.infrastructure.persistence;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespuestaVideoJpaRepository extends JpaRepository<RespuestaVideoJpaEntity, Long> {
    java.util.Optional<RespuestaVideoJpaEntity> findBySesionIdAndPreguntaId(Long sesionId, Integer preguntaId);
    @org.springframework.data.jpa.repository.Query("select r from RespuestaVideoJpaEntity r, SesionEntrevistaJpaEntity s where r.sesionId=s.id and r.id=:id and s.usuarioId=:usuario")
    java.util.Optional<RespuestaVideoJpaEntity> findPropia(@org.springframework.data.repository.query.Param("usuario") Long usuario, @org.springframework.data.repository.query.Param("id") Long id);
    List<RespuestaVideoJpaEntity> findBySesionIdOrderByIdAsc(Long sesionId);
    List<RespuestaVideoJpaEntity> findBySesionIdInOrderByIdAsc(Collection<Long> idsSesion);
}
