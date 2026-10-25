package pe.utp.entrevistapro360.application;
public class FormatoMultimediaNoAdmitidoException extends RuntimeException { public FormatoMultimediaNoAdmitidoException() { super("El archivo no tiene un formato admitido. Usa WebM VP8/VP9 con Opus o MP4 H.264 con AAC."); } }
