package pe.utp.entrevistapro360.domain;

import java.util.Map;

public final class FiltroHistorialInvalidoException extends RuntimeException {
    private final Map<String, String> campos;
    public FiltroHistorialInvalidoException(String mensaje, Map<String, String> campos) { super(mensaje); this.campos = Map.copyOf(campos); }
    public Map<String, String> campos() { return campos; }
}
