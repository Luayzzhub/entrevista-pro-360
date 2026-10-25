package pe.utp.entrevistapro360.application;
public class PreguntaNoHabilitadaException extends RuntimeException { public PreguntaNoHabilitadaException() { super("Esta pregunta aún no está habilitada. La respuesta anterior debe estar evaluada."); } }
