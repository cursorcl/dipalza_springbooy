package cl.eos.dipalza.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.eos.dipalza.model.ProductoDTO;
import cl.eos.dipalza.service.ProductoService;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

	private final ProductoService service;

	public ProductoController(ProductoService service) {
		this.service = service;
	}

//	// Listado paginado: /api/productos?page=0&size=20
//	@GetMapping
//	public Page<Producto> listar(@RequestParam(defaultValue = "0") int page,
//			@RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String q) {
//		Pageable pageable = PageRequest.of(page, size);
//		if (q != null && !q.isBlank()) {
//			return service.buscarPorDescripcion(q, pageable);
//		}
//		return service.listar(pageable);
//	}
	
    // Listado completo: opcionalmente filtra por q
    @GetMapping
    public List<ProductoDTO> getAllProductos() {
        return service.getAllProductos();
    }

	// Obtener por clave primaria (Articulo)
	@GetMapping("/{articulo}")
	public ResponseEntity<ProductoDTO> findProductoById(@PathVariable String articulo) {
		return service.findProductoById(articulo).
				map(ResponseEntity::ok)
				.orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
						org.springframework.http.HttpStatus.NOT_FOUND, "Producto no encontrado"));
	}
	
}
