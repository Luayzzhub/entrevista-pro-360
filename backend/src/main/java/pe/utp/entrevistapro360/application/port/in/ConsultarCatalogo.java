package pe.utp.entrevistapro360.application.port.in;

import java.util.List;
import pe.utp.entrevistapro360.domain.Pregunta;
import pe.utp.entrevistapro360.domain.Puesto;

public interface ConsultarCatalogo {
    List<Puesto> listarPuestos();
    List<Pregunta> listarPreguntas(int idPuesto);
}
