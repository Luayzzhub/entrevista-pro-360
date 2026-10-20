package pe.utp.entrevistapro360.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record Puesto(Integer idPuesto, String nombrePuesto, String area,
                     String descripcion, String nivelExperiencia, boolean activo) {
    public Puesto {
        if (idPuesto == null || idPuesto < 1 || nombrePuesto == null || nombrePuesto.isBlank()
                || area == null || area.isBlank() || nivelExperiencia == null || nivelExperiencia.isBlank()) {
            throw new IllegalArgumentException("El puesto requiere identidad, nombre, área y nivel de experiencia.");
        }
    }

    /** Invariante del MVP para consultar un catálogo habilitado e iniciar una futura práctica. */
    public void validarPreguntasActivas(List<Pregunta> preguntas) {
        if (!activo || preguntas == null || preguntas.size() != 3
                || preguntas.stream().anyMatch(p -> p == null || !p.activo() || !idPuesto.equals(p.idPuesto()))
                || !preguntas.stream().map(Pregunta::orden).collect(Collectors.toSet()).equals(Set.of(1, 2, 3))) {
            throw new CatalogoInvalidoException();
        }
    }
}
