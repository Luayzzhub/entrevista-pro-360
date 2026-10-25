package pe.utp.entrevistapro360.application;
public class AlmacenamientoNoDisponibleException extends RuntimeException { public AlmacenamientoNoDisponibleException() { super("No se pudo confirmar el almacenamiento privado. Conserva tu toma y vuelve a intentarlo."); } }
