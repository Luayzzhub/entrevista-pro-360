package pe.utp.entrevistapro360.support;
import java.util.*;
public final class EvaluacionFixtures {
    private EvaluacionFixtures() { }
    public static Map<String,Object> salida(boolean aplica) {
        Map<String,Object> criterios=new LinkedHashMap<>();
        var nombres=List.of("relevancia","claridad","estructura","evidencias");var puntajes=List.of(75,100,50,75);
        for(int i=0;i<4;i++)criterios.put(nombres.get(i),new LinkedHashMap<>(Map.of("puntaje",puntajes.get(i),"justificacion"," Justificación de "+nombres.get(i)+" ")));
        Map<String,Object> detalle=new LinkedHashMap<>();
        for(String k:List.of("situacion","tarea","accion","resultado"))detalle.put(k,new LinkedHashMap<>(Map.of("estado","PRESENTE","observacion"," Observación "+k+" ")));
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("version_contrato","EVAL-1.0");result.put("version_rubrica","COM-1.0");result.put("criterios",criterios);
        result.put("aplica_star",aplica);result.put("detalle_star",aplica?detalle:null);
        result.put("fortaleza_principal"," Fortaleza principal ");result.put("areas_mejora"," Síntesis de mejora ");result.put("recomendaciones"," Recomendaciones concretas ");return result;
    }
}

