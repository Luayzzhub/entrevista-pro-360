package pe.utp.entrevistapro360.domain;
import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class CalculadorMetricasTest {
    MetricaComunicacion calcular(String texto,String segundos) { return CalculadorMetricas.calcular(1L,texto,new BigDecimal(segundos),LocalDateTime.of(2026,10,25,12,0)); }
    @Test void unicodeNfkcGuionesApostrofosYSignos() {
        var m=calcular("Ｃódigo-fuente l'esprit d’Artagnan １２３ 中文 ‘hola’ -- - ' ’ !!!","60");
        assertThat(m.conteoPalabras()).isEqualTo(6); assertThat(m.palabrasPorMinuto()).isEqualByComparingTo("6.00");
        assertThat(m.detalleMuletillas()).isEmpty(); assertThat(m.versionMetricas()).isEqualTo("MET-1.0");
    }
    @Test void redondeoHalfUpUsaDuracionServidorSinRedondeoIntermedio() {
        assertThat(calcular("uno","32").palabrasPorMinuto()).isEqualTo(new BigDecimal("1.88"));
        assertThat(calcular("uno dos tres","7").palabrasPorMinuto()).isEqualTo(new BigDecimal("25.71"));
    }
    @Test void sieteEntradasCanonicasSinSubcadenasNiSolapamiento() {
        var m=calcular("EH ehh EHHH em MMM este O sea o sea o. Sea o, sea sistema estero eh-eh este’s ehhhh","60");
        assertThat(m.detalleMuletillas()).containsExactlyInAnyOrderEntriesOf(Map.of("eh",1,"ehh",1,"ehhh",1,"em",1,"mmm",1,"este",1,"o sea",2));
        assertThat(m.conteoMuletillas()).isEqualTo(8);
    }
    @Test void locucionSoloEspaciosNormalizadosIncluyeTabSaltoYNoPuntuacion() {
        var m=calcular("o\tsea o\nsea o\u00a0sea o—sea o/sea o-sea o’sea o sea o sea","60");
        assertThat(m.detalleMuletillas()).containsExactlyEntriesOf(Map.of("o sea",5));
    }
    @Test void omiteCerosYTotalEsSumaExacta() {
        var m=calcular("Este este eh mundo","2");
        assertThat(m.detalleMuletillas()).containsExactlyInAnyOrderEntriesOf(Map.of("este",2,"eh",1));
        assertThat(m.conteoMuletillas()).isEqualTo(3);
        assertThatThrownBy(()->m.detalleMuletillas().put("x",1)).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test void rechazaTextoSinPalabrasYDuracionNoPositiva() {
        for(String s:List.of("","   ","..., -- ’","😀")) assertThatThrownBy(()->calcular(s,"2")).isInstanceOf(TranscripcionSinPalabrasException.class);
        assertThatThrownBy(()->calcular("hola","0")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void comparacionNoDependeDeLocaleDelSistema() {
        Locale anterior=Locale.getDefault();
        try { Locale.setDefault(Locale.forLanguageTag("tr")); assertThat(calcular("ESTE EH O SEA","60").conteoMuletillas()).isEqualTo(3); }
        finally { Locale.setDefault(anterior); }
    }
}

