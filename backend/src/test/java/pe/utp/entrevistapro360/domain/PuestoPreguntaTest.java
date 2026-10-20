package pe.utp.entrevistapro360.domain;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PuestoPreguntaTest {
    private final Puesto puesto = new Puesto(7, "Soporte Técnico", "TI", null, "Inicial", true);

    private Pregunta pregunta(int id, int orden, boolean activa, int idPuesto) {
        return new Pregunta(id, idPuesto, "Enunciado", "Competencia", orden, 60, activa, orden == 2);
    }
    private List<Pregunta> validas() {
        return List.of(pregunta(1, 3, true, 7), pregunta(2, 1, true, 7), pregunta(3, 2, true, 7));
    }

    @Test void aceptaExactamenteTresPreguntasActivasSinExigirElOrdenDeEntrada() {
        assertThatCode(() -> puesto.validarPreguntasActivas(validas())).doesNotThrowAnyException();
    }
    @Test void rechazaCantidadIncompletaYSobrante() {
        assertThatThrownBy(() -> puesto.validarPreguntasActivas(validas().subList(0, 2))).isInstanceOf(CatalogoInvalidoException.class);
        var cuatro = new ArrayList<>(validas());
        cuatro.add(pregunta(4, 1, true, 7));
        assertThatThrownBy(() -> puesto.validarPreguntasActivas(cuatro)).isInstanceOf(CatalogoInvalidoException.class);
    }
    @Test void rechazaOrdenDuplicado() {
        assertThatThrownBy(() -> puesto.validarPreguntasActivas(List.of(pregunta(1, 1, true, 7),
                pregunta(2, 1, true, 7), pregunta(3, 3, true, 7)))).isInstanceOf(CatalogoInvalidoException.class);
    }
    @Test void rechazaPreguntaInactivaODeOtroPuesto() {
        assertThatThrownBy(() -> puesto.validarPreguntasActivas(List.of(pregunta(1, 1, true, 7),
                pregunta(2, 2, false, 7), pregunta(3, 3, true, 7)))).isInstanceOf(CatalogoInvalidoException.class);
        assertThatThrownBy(() -> puesto.validarPreguntasActivas(List.of(pregunta(1, 1, true, 8),
                pregunta(2, 2, true, 7), pregunta(3, 3, true, 7)))).isInstanceOf(CatalogoInvalidoException.class);
    }
    @Test void rechazaPuestoInactivoListaNulaYElementoNulo() {
        Puesto inactivo = new Puesto(7, "Soporte", "TI", null, "Inicial", false);
        assertThatThrownBy(() -> inactivo.validarPreguntasActivas(validas())).isInstanceOf(CatalogoInvalidoException.class);
        assertThatThrownBy(() -> puesto.validarPreguntasActivas(null)).isInstanceOf(CatalogoInvalidoException.class);
        assertThatThrownBy(() -> puesto.validarPreguntasActivas(Arrays.asList(null, validas().get(1), validas().get(2))))
                .isInstanceOf(CatalogoInvalidoException.class);
    }
    @Test void rechazaOrdenFueraDelMvpYDuracionDistintaDeSesenta() {
        assertThatThrownBy(() -> pregunta(1, 0, true, 7)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pregunta(1, 4, true, 7)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Pregunta(1, 7, "Texto", "Competencia", 1, 90, true, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
