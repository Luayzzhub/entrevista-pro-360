package pe.utp.entrevistapro360.presentation.dto;

/** Solo estos dos campos están permitidos; Jackson rechaza cualquier otro. */
public record ActualizarPerfilRequest(String nombreCompleto, String resumenPerfil) { }
