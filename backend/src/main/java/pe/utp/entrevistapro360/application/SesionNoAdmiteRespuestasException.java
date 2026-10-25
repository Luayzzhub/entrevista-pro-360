package pe.utp.entrevistapro360.application;
public class SesionNoAdmiteRespuestasException extends RuntimeException { public SesionNoAdmiteRespuestasException() { super("La sesión ya está cerrada y no admite nuevas respuestas."); } }
