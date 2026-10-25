package pe.utp.entrevistapro360.architecture;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class LayerDependenciesTest {
    @Test void dominioYAplicacionNoDependenDeFrameworksNiCapasExternas() throws Exception {
        for (String capa : java.util.List.of("domain", "application")) {
            try (var fuentes = Files.walk(Path.of("src/main/java/pe/utp/entrevistapro360", capa))) {
                for (Path fuente : fuentes.filter(p -> p.toString().endsWith(".java")).toList()) {
                    var imports = Files.readAllLines(fuente).stream().filter(l -> l.startsWith("import "))
                            .map(l -> l.replaceFirst("^import static ", "import ")).toList();
                    assertThat(imports).as(fuente.toString()).allMatch(l -> l.startsWith("import java.")
                            || l.startsWith("import pe.utp.entrevistapro360.domain.")
                            || (capa.equals("application") && l.startsWith("import pe.utp.entrevistapro360.application.")));
                }
            }
        }
    }
}
