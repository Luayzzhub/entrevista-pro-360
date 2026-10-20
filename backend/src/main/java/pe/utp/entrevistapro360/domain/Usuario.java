package pe.utp.entrevistapro360.domain;

import java.time.LocalDateTime;

/** Modelo canónico; registro y autenticación se implementan en el siguiente incremento. */
public record Usuario(Long idUsuario, String email, String passwordHash, String nombreCompleto,
                      Rol rol, Estado estado, LocalDateTime fechaRegistro,
                      LocalDateTime fechaActualizacion, String resumenPerfil) {
    public enum Rol { POSTULANTE, ADMIN, DOCENTE }
    public enum Estado { ACTIVO, INACTIVO, BLOQUEADO }
}
