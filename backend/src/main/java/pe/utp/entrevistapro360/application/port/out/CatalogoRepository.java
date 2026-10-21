package pe.utp.entrevistapro360.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.utp.entrevistapro360.domain.Pregunta;
import pe.utp.entrevistapro360.domain.Puesto;

public interface CatalogoRepository {
    List<Puesto> buscarPuestosActivos();
    Optional<Puesto> buscarPuestoActivo(int idPuesto);
    Optional<Puesto> buscarPuesto(int idPuesto);
    List<Pregunta> buscarPreguntasActivas(int idPuesto);
}
