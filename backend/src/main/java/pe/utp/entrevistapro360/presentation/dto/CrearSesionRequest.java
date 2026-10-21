package pe.utp.entrevistapro360.presentation.dto;

import jakarta.validation.constraints.*;

public record CrearSesionRequest(@NotNull(message = "Selecciona un puesto.") @Positive(message = "El puesto debe ser un entero positivo.") Integer idPuesto) { }
