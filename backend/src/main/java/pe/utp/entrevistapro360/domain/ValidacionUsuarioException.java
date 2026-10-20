package pe.utp.entrevistapro360.domain;

import java.util.Map;

public final class ValidacionUsuarioException extends RuntimeException {
    private final Map<String, String> erroresCampo;
    public ValidacionUsuarioException(Map<String, String> erroresCampo) {
        super("Revisa los campos indicados.");
        this.erroresCampo = Map.copyOf(erroresCampo);
    }
    public Map<String, String> erroresCampo() { return erroresCampo; }
}
