package pe.utp.entrevistapro360.presentation.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UsuarioResponse usuario) { }
