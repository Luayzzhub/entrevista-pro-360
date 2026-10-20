package pe.utp.entrevistapro360.domain;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/** Identidad inmutable; las fechas DATETIME se interpretan siempre como UTC. */
public record Usuario(Long idUsuario, String email, String passwordHash, String nombreCompleto,
                      Rol rol, Estado estado, LocalDateTime fechaRegistro,
                      LocalDateTime fechaActualizacion, String resumenPerfil) {
    private static final Pattern EMAIL = Pattern.compile("^[a-z0-9][a-z0-9._+%'-]*@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$");

    public Usuario {
        nombreCompleto = normalizarNombre(nombreCompleto);
        email = normalizarEmail(email);
        resumenPerfil = normalizarResumen(resumenPerfil);
        if (passwordHash == null || passwordHash.isBlank() || rol == null || estado == null || fechaRegistro == null) {
            throw new IllegalArgumentException("La identidad requiere hash, rol, estado y fecha de registro.");
        }
        if (idUsuario != null && idUsuario < 1) throw new IllegalArgumentException("Identificador inválido.");
    }

    public static Usuario registrar(String nombre, String email, String passwordHash, LocalDateTime ahoraUtc) {
        return new Usuario(null, email, passwordHash, nombre, Rol.POSTULANTE, Estado.ACTIVO, ahoraUtc, null, null);
    }

    public Usuario actualizarPerfil(String nombre, String resumen, LocalDateTime ahoraUtc) {
        return new Usuario(idUsuario, email, passwordHash, nombre, rol, estado, fechaRegistro,
                Objects.requireNonNull(ahoraUtc), resumen);
    }

    public boolean puedeAutenticarse() { return estado == Estado.ACTIVO && rol == Rol.POSTULANTE; }

    public static String normalizarNombre(String nombre) {
        String valor = nombre == null ? "" : nombre.trim();
        if (valor.isEmpty() || valor.length() > 150) invalidar("nombre_completo", "Ingresa un nombre de 1 a 150 caracteres.");
        return valor;
    }

    public static String normalizarEmail(String email) {
        String valor = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (valor.length() > 150 || !EMAIL.matcher(valor).matches() || valor.startsWith(".")
                || valor.contains("..") || valor.substring(0, valor.indexOf('@')).endsWith(".")) {
            invalidar("email", "Ingresa un correo válido de hasta 150 caracteres.");
        }
        return valor;
    }

    public static String normalizarResumen(String resumen) {
        String valor = resumen == null ? "" : resumen.trim();
        if (valor.length() > 500) invalidar("resumen_perfil", "El resumen admite hasta 500 caracteres.");
        return valor.isEmpty() ? null : valor;
    }

    /** Devuelve exactamente el mismo texto: ni trim ni normalización Unicode. */
    public static String validarPassword(String password) {
        if (password == null || password.codePointCount(0, password.length()) < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            invalidar("password", "Usa al menos 12 caracteres Unicode y como máximo 72 bytes UTF-8.");
        }
        return password;
    }

    public static void validarRegistro(String nombre, String email, String password) {
        Map<String, String> errores = new LinkedHashMap<>();
        capturar(() -> normalizarNombre(nombre), errores);
        capturar(() -> normalizarEmail(email), errores);
        capturar(() -> validarPassword(password), errores);
        if (!errores.isEmpty()) throw new ValidacionUsuarioException(errores);
    }

    private static void capturar(Runnable validacion, Map<String, String> errores) {
        try { validacion.run(); } catch (ValidacionUsuarioException ex) { errores.putAll(ex.erroresCampo()); }
    }
    private static void invalidar(String campo, String mensaje) {
        throw new ValidacionUsuarioException(Map.of(campo, mensaje));
    }

    public enum Rol { POSTULANTE, ADMIN, DOCENTE }
    public enum Estado { ACTIVO, INACTIVO, BLOQUEADO }
}
