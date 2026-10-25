package pe.utp.entrevistapro360.application;
public class PreguntaNoEncontradaException extends RuntimeException { public PreguntaNoEncontradaException() { super("La pregunta solicitada no existe en esta sesión."); } }
