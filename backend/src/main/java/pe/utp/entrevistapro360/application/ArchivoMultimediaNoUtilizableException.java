package pe.utp.entrevistapro360.application;
public class ArchivoMultimediaNoUtilizableException extends RuntimeException { public ArchivoMultimediaNoUtilizableException() { super("El video debe incluir imagen y audio y durar entre 1 y 60 segundos."); } }
