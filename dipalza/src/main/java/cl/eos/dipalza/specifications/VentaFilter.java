package cl.eos.dipalza.specifications;

import java.time.LocalDate;
import java.util.List;

public record VentaFilter(
		List<String> estados,
	    List<String> rutsClientes,       // Basado en el campo 'rut' de ClienteId
	    List<String> codigosRutas,
	    List<Long> condicionVentaIds,    // IDs de Condición de Venta
	    List<String> codigosVendedores,
	    LocalDate fechaInicio,
	    LocalDate fechaFin
	) {}