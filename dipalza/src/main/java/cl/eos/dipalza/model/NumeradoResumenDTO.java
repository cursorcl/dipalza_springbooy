package cl.eos.dipalza.model;

import java.math.BigDecimal;

public record NumeradoResumenDTO(String codigoProducto, String nombreProducto, BigDecimal peso, Long piezas ) {
}
