package pe.utp.entrevistapro360.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record ApiError(String codigo, String mensaje, @JsonProperty("errores_campo") Map<String, String> erroresCampo) {
    public static ApiError of(String codigo, String mensaje) { return new ApiError(codigo, mensaje, Map.of()); }
}
