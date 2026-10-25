package pe.utp.entrevistapro360.domain;

import java.math.*;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

public final class CalculadorMetricas {
    private static final Pattern PALABRA = Pattern.compile("[\\p{L}\\p{Nd}]+(?:[-'’][\\p{L}\\p{Nd}]+)*");
    private static final List<String> CATALOGO = List.of("eh", "ehh", "ehhh", "em", "mmm", "este", "o sea");
    private record Token(String texto, int inicio, int fin) { }
    private CalculadorMetricas() { }
    public static MetricaComunicacion calcular(Long idRespuesta, String transcripcion, BigDecimal duracion, LocalDateTime ahora) {
        if (duracion == null || duracion.signum() <= 0) throw new IllegalArgumentException("Duración positiva requerida.");
        String texto = Normalizer.normalize(Objects.requireNonNull(transcripcion), Normalizer.Form.NFKC);
        List<Token> tokens = new ArrayList<>(); var matcher = PALABRA.matcher(texto);
        while (matcher.find()) tokens.add(new Token(matcher.group().toLowerCase(Locale.ROOT), matcher.start(), matcher.end()));
        if (tokens.isEmpty()) throw new TranscripcionSinPalabrasException();
        Map<String, Integer> conteos = new LinkedHashMap<>(); CATALOGO.forEach(s -> conteos.put(s, 0));
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.texto.equals("o") && i + 1 < tokens.size() && tokens.get(i + 1).texto.equals("sea")
                    && texto.substring(t.fin, tokens.get(i + 1).inicio).codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
                conteos.merge("o sea", 1, Integer::sum); i++;
            } else if (conteos.containsKey(t.texto)) conteos.merge(t.texto, 1, Integer::sum);
        }
        Map<String, Object> detalle = new LinkedHashMap<>();
        conteos.forEach((key, value) -> { if (value > 0) detalle.put(key, value); });
        int total = conteos.values().stream().mapToInt(Integer::intValue).sum();
        BigDecimal ppm = BigDecimal.valueOf(tokens.size()).multiply(BigDecimal.valueOf(60)).divide(duracion, 2, RoundingMode.HALF_UP);
        return new MetricaComunicacion(null, idRespuesta, tokens.size(), ppm, total, Collections.unmodifiableMap(detalle), "MET-1.0", ahora.withNano(0));
    }
}
