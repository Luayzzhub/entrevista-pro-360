package pe.utp.entrevistapro360.application;

import java.util.Comparator;
import java.util.List;
import pe.utp.entrevistapro360.application.port.in.ConsultarCatalogo;
import pe.utp.entrevistapro360.application.port.out.CatalogoRepository;
import pe.utp.entrevistapro360.domain.Pregunta;
import pe.utp.entrevistapro360.domain.Puesto;

public final class ConsultarCatalogoService implements ConsultarCatalogo {
    private final CatalogoRepository catalogo;

    public ConsultarCatalogoService(CatalogoRepository catalogo) { this.catalogo = catalogo; }

    @Override
    public List<Puesto> listarPuestos() { return catalogo.buscarPuestosActivos(); }

    @Override
    public List<Pregunta> listarPreguntas(int idPuesto) {
        Puesto puesto = catalogo.buscarPuestoActivo(idPuesto).orElseThrow(PuestoNoEncontradoException::new);
        List<Pregunta> preguntas = catalogo.buscarPreguntasActivas(idPuesto);
        puesto.validarPreguntasActivas(preguntas);
        return preguntas.stream().sorted(Comparator.comparingInt(Pregunta::orden)).toList();
    }
}
