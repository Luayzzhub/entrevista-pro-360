package pe.utp.entrevistapro360.domain;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;
public final class ValidadorEvaluacion {
    public static final List<String> CRITERIOS=List.of("relevancia","claridad","estructura","evidencias");
    public static final List<String> STAR=List.of("situacion","tarea","accion","resultado");
    private ValidadorEvaluacion() { }
    public static EvaluacionSTAR validar(Long id, Map<String,Object> salida, boolean aplica, LocalDateTime fecha) {
        exacto(salida,Set.of("version_contrato","version_rubrica","criterios","aplica_star","detalle_star","fortaleza_principal","areas_mejora","recomendaciones"));
        exigir("EVAL-1.0".equals(salida.get("version_contrato")) && "COM-1.0".equals(salida.get("version_rubrica")));
        exigir(Boolean.valueOf(aplica).equals(salida.get("aplica_star")));
        var criterios=mapa(salida.get("criterios"));exacto(criterios,Set.copyOf(CRITERIOS));
        List<BigDecimal> puntos=new ArrayList<>();Map<String,Object> justificaciones=new LinkedHashMap<>();
        for(String nombre:CRITERIOS) {
            var c=mapa(criterios.get(nombre));exacto(c,Set.of("puntaje","justificacion"));
            Object n=c.get("puntaje");exigir(n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte || n instanceof BigInteger);
            BigDecimal p=new BigDecimal(n.toString());exigir(Set.of(BigDecimal.ZERO,new BigDecimal("25"),new BigDecimal("50"),new BigDecimal("75"),new BigDecimal("100")).contains(p));
            puntos.add(p.setScale(2));justificaciones.put(nombre,texto(c.get("justificacion"),600));
        }
        Map<String,Object> detalle=null;
        if(!aplica) exigir(salida.get("detalle_star")==null);
        else {
            var recibido=mapa(salida.get("detalle_star"));exacto(recibido,Set.copyOf(STAR));detalle=new LinkedHashMap<>();
            for(String nombre:STAR) {
                var elemento=mapa(recibido.get(nombre));exacto(elemento,Set.of("estado","observacion"));
                exigir(elemento.get("estado") instanceof String && Set.of("PRESENTE","PARCIAL","AUSENTE").contains(elemento.get("estado")));
                detalle.put(nombre,Map.of("estado",elemento.get("estado"),"observacion",texto(elemento.get("observacion"),600)));
            }
            detalle=Map.copyOf(detalle);
        }
        var areas=Map.<String,Object>of("version_contrato","EVAL-1.0","justificaciones",Map.copyOf(justificaciones),"sintesis",texto(salida.get("areas_mejora"),1200));
        BigDecimal general=puntos.stream().reduce(BigDecimal.ZERO,BigDecimal::add).divide(BigDecimal.valueOf(4),2,RoundingMode.HALF_UP);
        return new EvaluacionSTAR(null,id,general,puntos.get(0),puntos.get(1),puntos.get(2),puntos.get(3),aplica,detalle,
                "COM-1.0",texto(salida.get("fortaleza_principal"),1200),areas,texto(salida.get("recomendaciones"),1200),fecha.withNano(0));
    }
    public static EvaluacionSTAR validarGuardada(EvaluacionSTAR e,boolean aplica) {
        try {
            exigir(e.areasMejora()!=null);exacto(e.areasMejora(),Set.of("version_contrato","justificaciones","sintesis"));
            Map<String,Object> criterios=new LinkedHashMap<>();var j=mapa(e.areasMejora().get("justificaciones"));exacto(j,Set.copyOf(CRITERIOS));
            var puntos=List.of(e.puntajeRelevancia(),e.puntajeClaridad(),e.puntajeEstructura(),e.puntajeEvidencias());
            for(int i=0;i<4;i++) criterios.put(CRITERIOS.get(i),Map.of("puntaje",puntos.get(i).toBigIntegerExact(),"justificacion",j.get(CRITERIOS.get(i))));
            Map<String,Object> salida=new LinkedHashMap<>();
            salida.put("version_contrato",e.areasMejora().get("version_contrato"));salida.put("version_rubrica",e.versionRubrica());
            salida.put("criterios",criterios);salida.put("aplica_star",e.aplicaStar());salida.put("detalle_star",e.detalleStar());
            salida.put("fortaleza_principal",e.fortalezaPrincipal());salida.put("areas_mejora",e.areasMejora().get("sintesis"));salida.put("recomendaciones",e.recomendaciones());
            var valida=validar(e.idRespuesta(),salida,aplica,e.fechaEvaluacion());
            exigir(valida.puntajeGeneral().compareTo(e.puntajeGeneral())==0);return e;
        } catch(NullPointerException | ArithmeticException ex) { throw new SalidaEvaluacionInvalidaException(); }
    }
    private static String texto(Object value,int max) { exigir(value instanceof String);String s=((String)value).trim();exigir(!s.isBlank() && s.length()<=max);return s; }
    @SuppressWarnings("unchecked") private static Map<String,Object> mapa(Object value) { exigir(value instanceof Map<?,?>);return (Map<String,Object>)value; }
    private static void exacto(Map<String,Object> value,Set<String> keys) { exigir(value!=null && value.keySet().equals(keys)); }
    private static void exigir(boolean condicion) { if(!condicion) throw new SalidaEvaluacionInvalidaException(); }
}

