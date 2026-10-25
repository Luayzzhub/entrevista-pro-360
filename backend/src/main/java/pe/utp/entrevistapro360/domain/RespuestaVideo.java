package pe.utp.entrevistapro360.domain;

import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

public record RespuestaVideo(Long idRespuesta, Long idSesion, Integer idPregunta,
                            String publicIdCloud, BigDecimal duracionSegundos, String formato,
                            Long tamanoBytes, EstadoProcesamiento estadoProcesamiento,
                            LocalDateTime fechaGrabacion, Map<String, Object> procesamientoContexto) {
    public static final long MAXIMO_TAMANO_BYTES = 50_000_000L;
    public RespuestaVideo {
        if ((idRespuesta != null && idRespuesta < 1) || idSesion == null || idSesion < 1 || idPregunta == null || idPregunta < 1)
            throw new IllegalArgumentException("Identidad de respuesta inválida.");
        if (publicIdCloud == null || publicIdCloud.isBlank() || publicIdCloud.length() > 150 || publicIdCloud.contains("..")
                || publicIdCloud.toLowerCase(Locale.ROOT).startsWith("http://") || publicIdCloud.toLowerCase(Locale.ROOT).startsWith("https://")
                || publicIdCloud.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c) || Character.isISOControl(c)))
            throw new IllegalArgumentException("La referencia privada no es válida.");
        if (!Set.of("webm", "mp4").contains(formato == null ? "" : formato)) throw new IllegalArgumentException("Formato inválido.");
        if (tamanoBytes == null || tamanoBytes <= 0 || tamanoBytes >= MAXIMO_TAMANO_BYTES) throw new IllegalArgumentException("Tamaño inválido.");
        if (duracionSegundos == null || duracionSegundos.compareTo(BigDecimal.ONE) < 0 || duracionSegundos.compareTo(new BigDecimal("60")) > 0)
            throw new IllegalArgumentException("Duración fuera de 1 a 60 segundos.");
        duracionSegundos = duracionSegundos.setScale(3, RoundingMode.HALF_UP);
        if (estadoProcesamiento == null || fechaGrabacion == null || procesamientoContexto == null || procesamientoContexto.isEmpty())
            throw new IllegalArgumentException("Estado, fecha y contexto de procesamiento son obligatorios.");
        fechaGrabacion = fechaGrabacion.withNano(0);
        procesamientoContexto = copiarMapa(procesamientoContexto);
    }
    public static RespuestaVideo confirmarSubida(Long idSesion, Integer idPregunta, String clave, BigDecimal duracion,
            String formato, long bytes, LocalDateTime ahoraUtc, String modeloStt, String modeloLlm) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("version_contexto", "PROC-1.0"); c.put("etapa_actual", "EXTRAER_AUDIO"); c.put("ultimo_hito_valido", "SUBIDO"); c.put("ciclo_actual", 1);
        Map<String, Object> intentos = new LinkedHashMap<>();
        for (String etapa : List.of("EXTRAER_AUDIO", "TRANSCRIBIR", "METRICAS", "EVALUAR")) intentos.put(etapa, Map.of("ciclo_1", 0, "ciclo_2", 0));
        c.put("intentos_por_etapa", intentos); c.put("reintento_manual_usado", false);
        c.put("ultimo_error", null); c.put("proximo_intento_utc", null); c.put("bloqueo_hasta_utc", null);
        c.put("version_ejecucion", 0); c.put("referencia_audio", null);
        c.put("modelo_stt", Objects.requireNonNull(modeloStt)); c.put("modelo_llm", Objects.requireNonNull(modeloLlm));
        return new RespuestaVideo(null, idSesion, idPregunta, clave, duracion, formato, bytes, EstadoProcesamiento.SUBIDO, ahoraUtc, c);
    }
    private static Map<String, Object> copiarMapa(Map<String, Object> origen) {
        Map<String, Object> copia = new LinkedHashMap<>();
        origen.forEach((key, value) -> copia.put(key, copiarValor(value)));
        return Collections.unmodifiableMap(copia);
    }
    private static Object copiarValor(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> copia = new LinkedHashMap<>();
            mapa.forEach((key, value) -> copia.put((String) key, copiarValor(value)));
            return Collections.unmodifiableMap(copia);
        }
        if (valor instanceof List<?> lista) return Collections.unmodifiableList(lista.stream().map(RespuestaVideo::copiarValor).toList());
        return valor;
    }
    public enum EstadoProcesamiento { SUBIDO, AUDIO_EXTRAIDO, TRANSCRITO, EVALUADO, FALLIDO }
}
