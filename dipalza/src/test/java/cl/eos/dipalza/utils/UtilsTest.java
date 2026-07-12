package cl.eos.dipalza.utils;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UtilsTest {

    @Test
    void putZeroesAtBegin_ceroConLen4_retornaCuatroCeros() {
        assertThat(Utils.putZeroesAtBegin(0, 4)).isEqualTo("0000");
    }

    @Test
    void putZeroesAtBegin_numeroMenorQueLen_rellenaConCeros() {
        assertThat(Utils.putZeroesAtBegin(7, 3)).isEqualTo("007");
    }

    @Test
    void putZeroesAtBegin_numeroIgualAlLen_noAgregueCeros() {
        assertThat(Utils.putZeroesAtBegin(123, 3)).isEqualTo("123");
    }

    @Test
    void putStrAtBegin_sourceMenorQueLen_rellenaPorLaIzquierda() {
        assertThat(Utils.putStrAtBegin("AB", ' ', 5)).isEqualTo("   AB");
    }

    @Test
    void putStrAtBegin_sourceIgualAlLen_noModifica() {
        assertThat(Utils.putStrAtBegin("ABC", 'X', 3)).isEqualTo("ABC");
    }

    @Test
    void putStrAtBegin_sourceMayorQueLen_retornaOriginal() {
        assertThat(Utils.putStrAtBegin("ABCDE", 'X', 3)).isEqualTo("ABCDE");
    }
}
