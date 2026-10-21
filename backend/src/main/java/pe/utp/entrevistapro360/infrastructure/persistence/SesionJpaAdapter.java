package pe.utp.entrevistapro360.infrastructure.persistence;

import java.util.*;
import java.util.function.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import pe.utp.entrevistapro360.application.*;
import pe.utp.entrevistapro360.application.port.out.SesionRepository;
import pe.utp.entrevistapro360.domain.*;

@Repository
@Transactional(readOnly = true)
public class SesionJpaAdapter implements SesionRepository {
    private final UsuarioJpaRepository usuarios;
    private final SesionEntrevistaJpaRepository sesiones;
    private final RespuestaVideoJpaRepository respuestas;
    public SesionJpaAdapter(UsuarioJpaRepository usuarios, SesionEntrevistaJpaRepository sesiones, RespuestaVideoJpaRepository respuestas) {
        this.usuarios = usuarios; this.sesiones = sesiones; this.respuestas = respuestas;
    }
    @Override @Transactional(isolation = Isolation.READ_COMMITTED)
    public <T> T conUsuarioBloqueado(long idUsuario, Supplier<T> accion) {
        // Primera lectura de esta transacción: el lock cubre búsqueda, regla, INSERT y commit.
        Usuario usuario = usuarios.findByIdForUpdate(idUsuario).map(UsuarioJpaEntity::toDomain).orElseThrow(CredencialesInvalidasException::new);
        if (!usuario.puedeAutenticarse()) throw new CredencialesInvalidasException();
        return accion.get();
    }
    @Override @Transactional
    public <T> T conSesionBloqueada(long idUsuario, long idSesion, Function<SesionEntrevista, T> accion) {
        SesionEntrevista sesion = sesiones.findPropiaForUpdate(idSesion, idUsuario).map(SesionEntrevistaJpaEntity::toDomain)
                .orElseThrow(SesionNoEncontradaException::new);
        return accion.apply(sesion);
    }
    @Override public <T> T enLectura(Supplier<T> accion) { return accion.get(); }
    @Override public Optional<SesionEntrevista> buscarNoTerminal(long idUsuario) {
        return sesiones.findFirstByUsuarioIdAndEstadoSesionInOrderByFechaInicioDescIdDesc(idUsuario,
                List.of(SesionEntrevista.EstadoSesion.INICIADA, SesionEntrevista.EstadoSesion.EN_PROCESO)).map(SesionEntrevistaJpaEntity::toDomain);
    }
    @Override public Optional<SesionEntrevista> buscarPropia(long idUsuario, long idSesion) {
        return sesiones.findByIdAndUsuarioId(idSesion, idUsuario).map(SesionEntrevistaJpaEntity::toDomain);
    }
    @Override @Transactional public SesionEntrevista guardarNueva(SesionEntrevista sesion) {
        return sesiones.saveAndFlush(SesionEntrevistaJpaEntity.nueva(sesion)).toDomain();
    }
    @Override @Transactional public SesionEntrevista actualizar(SesionEntrevista sesion) {
        var entity = sesiones.findByIdAndUsuarioId(sesion.idSesion(), sesion.idUsuario()).orElseThrow(SesionNoEncontradaException::new);
        entity.actualizarEstado(sesion); return sesiones.saveAndFlush(entity).toDomain();
    }
    @Override public List<SesionEntrevista> listarPropias(long idUsuario) {
        return sesiones.findByUsuarioIdOrderByFechaInicioDescIdDesc(idUsuario).stream().map(SesionEntrevistaJpaEntity::toDomain).toList();
    }
    @Override public List<RespuestaVideo> buscarRespuestas(Collection<Long> idsSesion) {
        return idsSesion.isEmpty() ? List.of() : respuestas.findBySesionIdInOrderByIdAsc(idsSesion).stream().map(RespuestaVideoJpaEntity::toDomain).toList();
    }
}
