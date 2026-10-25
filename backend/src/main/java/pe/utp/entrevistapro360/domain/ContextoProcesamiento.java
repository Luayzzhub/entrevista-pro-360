package pe.utp.entrevistapro360.domain;

import java.time.*;
import java.util.*;
import static pe.utp.entrevistapro360.domain.RespuestaVideo.EstadoProcesamiento.*;

public final class ContextoProcesamiento {
    public enum Etapa { EXTRAER_AUDIO, TRANSCRIBIR, METRICAS, EVALUAR }
    private final RespuestaVideo respuesta;
    private final Map<String, Object> datos;
    public ContextoProcesamiento(RespuestaVideo respuesta) {
        this.respuesta = respuesta; this.datos = new LinkedHashMap<>(respuesta.procesamientoContexto());
        if (!"PROC-1.0".equals(datos.get("version_contexto"))) throw new IllegalArgumentException("Contexto PROC-1.0 requerido.");
    }
    public static boolean compatible(RespuestaVideo r) { return "PROC-1.0".equals(r.procesamientoContexto().get("version_contexto")); }
    public Etapa etapa() { return Etapa.valueOf((String) datos.get("etapa_actual")); }
    public long version() { return ((Number) datos.get("version_ejecucion")).longValue(); }
    public int ciclo() { return ((Number) datos.get("ciclo_actual")).intValue(); }
    public String hito() { return (String) datos.get("ultimo_hito_valido"); }
    public String audio() { return (String) datos.get("referencia_audio"); }
    public String modeloStt() { return (String) datos.get("modelo_stt"); }
    public int intentos() { return ((Number) mapa(mapa(datos.get("intentos_por_etapa")).get(etapa().name())).get("ciclo_" + ciclo())).intValue(); }
    public boolean agotado() { return intentos() >= 3; }
    public boolean tieneLease() { return datos.get("bloqueo_hasta_utc") != null; }
    public boolean definitivo() { return respuesta.estadoProcesamiento() == FALLIDO && !recuperable(); }
    public boolean recuperable() { return datos.get("ultimo_error") instanceof Map<?, ?> error && Boolean.TRUE.equals(error.get("recuperable")); }
    public boolean leaseLibre(LocalDateTime ahora) {
        LocalDateTime lease = fecha("bloqueo_hasta_utc"); return lease == null || lease.isBefore(ahora);
    }
    public boolean elegible(SesionEntrevista sesion, LocalDateTime ahora) {
        LocalDateTime proximo = fecha("proximo_intento_utc");
        return !sesion.esTerminal() && Set.of(SUBIDO, AUDIO_EXTRAIDO, TRANSCRITO).contains(respuesta.estadoProcesamiento())
                && etapa() != Etapa.EVALUAR && leaseLibre(ahora) && (proximo == null || !proximo.isAfter(ahora));
    }
    public RespuestaVideo reclamar(LocalDateTime ahora) {
        if (agotado()) throw new IllegalStateException("No se permite un cuarto intento del ciclo.");
        Map<String, Object> etapas = new LinkedHashMap<>(mapa(datos.get("intentos_por_etapa")));
        Map<String, Object> contadores = new LinkedHashMap<>(mapa(etapas.get(etapa().name())));
        contadores.put("ciclo_" + ciclo(), intentos() + 1); etapas.put(etapa().name(), contadores);
        datos.put("intentos_por_etapa", etapas); datos.put("version_ejecucion", version() + 1);
        datos.put("bloqueo_hasta_utc", utc(ahora.plusSeconds(90)));
        return construir(respuesta.estadoProcesamiento());
    }
    public RespuestaVideo agotarLease(LocalDateTime ahora, long base1, long base2) {
        datos.put("version_ejecucion", version() + 1);
        return fallar("INTENTOS_AGOTADOS", true, ahora, 0, base1, base2);
    }
    public RespuestaVideo renovar(LocalDateTime ahora) { datos.put("bloqueo_hasta_utc", utc(ahora.plusSeconds(90))); return construir(respuesta.estadoProcesamiento()); }
    public RespuestaVideo liberar() { datos.put("bloqueo_hasta_utc", null); return construir(respuesta.estadoProcesamiento()); }
    public RespuestaVideo audioExtraido(String referencia) {
        datos.put("referencia_audio", referencia);
        if (hito().equals("TRANSCRITO")) return exito(TRANSCRITO, Etapa.METRICAS);
        return exito(AUDIO_EXTRAIDO, Etapa.TRANSCRIBIR);
    }
    public RespuestaVideo transcrito() { return exito(TRANSCRITO, Etapa.METRICAS); }
    public RespuestaVideo metricasCompletas() { return exito(TRANSCRITO, Etapa.EVALUAR); }
    private RespuestaVideo exito(RespuestaVideo.EstadoProcesamiento estado, Etapa siguiente) {
        datos.put("ultimo_hito_valido", estado.name()); datos.put("etapa_actual", siguiente.name());
        datos.put("ultimo_error", null); datos.put("proximo_intento_utc", null); datos.put("bloqueo_hasta_utc", null);
        return construir(estado);
    }
    public RespuestaVideo fallar(String codigo, boolean recuperable, LocalDateTime ahora, long retryAfter, long base1, long base2) {
        boolean definitiva = !recuperable || (ciclo() == 2 && agotado());
        datos.put("ultimo_error", Map.of("codigo", codigo, "etapa", etapa().name(), "recuperable", !definitiva, "fecha_utc", utc(ahora)));
        datos.put("bloqueo_hasta_utc", null);
        datos.put("proximo_intento_utc", recuperable && !agotado() ? utc(ahora.plusSeconds(Math.max(intentos() == 1 ? base1 : base2, retryAfter))) : null);
        return construir(definitiva || agotado() ? FALLIDO : respuesta.estadoProcesamiento());
    }
    public boolean puedeReintentar(SesionEntrevista sesion, LocalDateTime ahora) {
        return sesion.estadoSesion() == SesionEntrevista.EstadoSesion.EN_PROCESO && respuesta.estadoProcesamiento() == FALLIDO
                && recuperable() && !Boolean.TRUE.equals(datos.get("reintento_manual_usado")) && ciclo() == 1 && leaseLibre(ahora);
    }
    public RespuestaVideo reintentar(SesionEntrevista sesion, LocalDateTime ahora) {
        if (!puedeReintentar(sesion, ahora)) throw new ReintentoNoDisponibleException();
        datos.put("reintento_manual_usado", true); datos.put("ciclo_actual", 2); datos.put("ultimo_error", null);
        datos.put("proximo_intento_utc", utc(ahora)); datos.put("bloqueo_hasta_utc", null);
        return construir(RespuestaVideo.EstadoProcesamiento.valueOf(hito()));
    }
    private LocalDateTime fecha(String campo) {
        Object valor = datos.get(campo);
        return valor == null ? null : OffsetDateTime.parse(valor.toString()).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
    public static String utc(LocalDateTime fecha) { return fecha.withNano(0).atOffset(ZoneOffset.UTC).toString(); }
    @SuppressWarnings("unchecked") private static Map<String, Object> mapa(Object valor) { return (Map<String, Object>) valor; }
    private RespuestaVideo construir(RespuestaVideo.EstadoProcesamiento estado) {
        return new RespuestaVideo(respuesta.idRespuesta(), respuesta.idSesion(), respuesta.idPregunta(), respuesta.publicIdCloud(),
                respuesta.duracionSegundos(), respuesta.formato(), respuesta.tamanoBytes(), estado, respuesta.fechaGrabacion(), datos);
    }
}
