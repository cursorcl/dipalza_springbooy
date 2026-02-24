package cl.eos.dipalza.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import cl.eos.dipalza.entity.EstadoVenta;
import cl.eos.dipalza.entity.Venta;
import jakarta.persistence.criteria.Predicate;

public class VentaSpecifications {

	public static Specification<Venta> toSpecification(VentaFilter filter) {
	    return (root, query, cb) -> {
	        List<Predicate> predicates = new ArrayList<>();

	        // 1. Estados: Conversión segura de String a Enum
	        if (filter.estados() != null && !filter.estados().isEmpty()) {
	            List<EstadoVenta> estadosEnum = filter.estados().stream()
	                .map(String::toUpperCase) // Aseguramos consistencia
	                .map(EstadoVenta::valueOf)
	                .toList();
	            predicates.add(root.get("estado").in(estadosEnum));
	        }

	        // 2. Clientes: Navegación por la clave compuesta ClienteId
	        if (filter.rutsClientes() != null && !filter.rutsClientes().isEmpty()) {
	            // Accedemos a la PK empotrada 'id' y luego al campo 'rut'
	            predicates.add(root.get("cliente").get("id").get("rut").in(filter.rutsClientes()));
	        }

	        // 3. Condición de Venta
	        if (filter.condicionVentaIds() != null && !filter.condicionVentaIds().isEmpty()) {
	            predicates.add(root.get("condicionVenta").get("id").in(filter.condicionVentaIds()));
	        }

	        // 4. Ruta
	        if (filter.codigosRutas() != null && !filter.codigosRutas().isEmpty()) {
	            predicates.add(root.get("ruta").get("codigo").in(filter.codigosRutas()));
	        }

	        // 5. Rango de fechas
	        if (filter.fechaInicio() != null && filter.fechaFin() != null) {
	            predicates.add(cb.between(root.get("fecha"), filter.fechaInicio(), filter.fechaFin()));
	        }

	        return cb.and(predicates.toArray(new Predicate[0]));
	    };
	}
}