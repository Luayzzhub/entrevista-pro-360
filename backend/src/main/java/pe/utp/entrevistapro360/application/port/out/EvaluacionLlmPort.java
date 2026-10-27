package pe.utp.entrevistapro360.application.port.out;
import java.util.Map;
import pe.utp.entrevistapro360.application.EntradaEvaluacion;
public interface EvaluacionLlmPort { Map<String,Object> evaluar(EntradaEvaluacion entrada,String modelo); }

