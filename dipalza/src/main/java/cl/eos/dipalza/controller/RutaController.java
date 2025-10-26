package cl.eos.dipalza.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.eos.dipalza.model.RutaDTO;
import cl.eos.dipalza.service.RutaService;

@RestController
@RequestMapping("/api/rutas")
public class RutaController {

    private final RutaService rutaService;

    public RutaController(RutaService rutaService) {
        this.rutaService = rutaService;
    }

    @GetMapping
    public List<RutaDTO> getAllRutas() {
        return rutaService.getAllRutas();
    }
    
    @GetMapping("/{codigo}")
    public ResponseEntity<RutaDTO> getRutaById(@PathVariable String codigo) {
    	return rutaService.getRutaById(codigo)
    			.map(ResponseEntity::ok)
    			.orElse(ResponseEntity.notFound().build());
    }
}
