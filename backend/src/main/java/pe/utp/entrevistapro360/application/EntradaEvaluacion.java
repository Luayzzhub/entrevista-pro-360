package pe.utp.entrevistapro360.application;
import java.util.Map;
public record EntradaEvaluacion(String puesto,String pregunta,String competenciaEvaluada,boolean aplicaStar,String transcripcion) {
    public Map<String,Object> payload() { return Map.of("version_contrato","EVAL-1.0","version_rubrica","COM-1.0","puesto",puesto,"pregunta",pregunta,
            "competencia_evaluada",competenciaEvaluada,"aplica_star",aplicaStar,"transcripcion",transcripcion); }
}

