package pe.utp.entrevistapro360.domain;
public class ReintentoNoDisponibleException extends RuntimeException { public ReintentoNoDisponibleException() { super("El reintento manual no está disponible para esta respuesta."); } }
