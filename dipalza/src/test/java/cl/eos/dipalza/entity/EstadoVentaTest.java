package cl.eos.dipalza.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoVentaTest {

    @Test
    void fromName_estadoValido_retornaEnum() {
        assertEquals(EstadoVenta.FINISHED, EstadoVenta.fromName("FINISHED"));
        assertEquals(EstadoVenta.CLOSED,   EstadoVenta.fromName("closed"));
        assertEquals(EstadoVenta.OPENED,   EstadoVenta.fromName("OPENED"));
    }

    @Test
    void fromName_nulo_retornaOpened() {
        assertEquals(EstadoVenta.OPENED, EstadoVenta.fromName(null));
        assertEquals(EstadoVenta.OPENED, EstadoVenta.fromName("   "));
    }

    @Test
    void fromName_estadoInvalido_retornaNull() {
        assertNull(EstadoVenta.fromName("INVALID_STATE"));
        assertNull(EstadoVenta.fromName("UNKNOWN"));
    }
}
