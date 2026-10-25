package pe.utp.entrevistapro360.application;
public class FalloEtapaException extends RuntimeException {
    private final String codigo; private final boolean recuperable; private final long retryAfter;
    public FalloEtapaException(String codigo, boolean recuperable) { this(codigo, recuperable, 0); }
    public FalloEtapaException(String codigo, boolean recuperable, long retryAfter) {
        super("No se pudo completar la etapa de procesamiento."); this.codigo=codigo; this.recuperable=recuperable; this.retryAfter=Math.max(0,retryAfter);
    }
    public String codigo() { return codigo; } public boolean recuperable() { return recuperable; } public long retryAfter() { return retryAfter; }
}
