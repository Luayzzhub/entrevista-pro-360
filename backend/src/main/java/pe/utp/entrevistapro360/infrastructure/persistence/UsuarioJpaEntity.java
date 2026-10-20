package pe.utp.entrevistapro360.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import pe.utp.entrevistapro360.domain.Usuario;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(name = "uk_usuarios_email", columnNames = "email"))
public class UsuarioJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_usuario") private Long id;
    @Column(nullable = false, length = 150) private String email;
    @Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;
    @Column(name = "nombre_completo", nullable = false, length = 150) private String nombreCompleto;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Usuario.Rol rol;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Usuario.Estado estado;
    @Column(name = "fecha_registro", nullable = false) private LocalDateTime fechaRegistro;
    @Column(name = "fecha_actualizacion") private LocalDateTime fechaActualizacion;
    @Column(name = "resumen_perfil", length = 500) private String resumenPerfil;

    protected UsuarioJpaEntity() { }
    static UsuarioJpaEntity nuevo(Usuario usuario) {
        UsuarioJpaEntity entity = new UsuarioJpaEntity();
        entity.email = usuario.email(); entity.passwordHash = usuario.passwordHash();
        entity.nombreCompleto = usuario.nombreCompleto(); entity.rol = usuario.rol(); entity.estado = usuario.estado();
        entity.fechaRegistro = usuario.fechaRegistro(); entity.fechaActualizacion = usuario.fechaActualizacion();
        entity.resumenPerfil = usuario.resumenPerfil();
        return entity;
    }
    void actualizarPerfil(Usuario usuario) {
        nombreCompleto = usuario.nombreCompleto(); resumenPerfil = usuario.resumenPerfil();
        fechaActualizacion = usuario.fechaActualizacion();
    }
    Usuario toDomain() {
        return new Usuario(id, email, passwordHash, nombreCompleto, rol, estado, fechaRegistro, fechaActualizacion, resumenPerfil);
    }
}
