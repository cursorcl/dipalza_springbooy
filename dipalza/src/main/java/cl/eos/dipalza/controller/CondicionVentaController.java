package cl.eos.dipalza.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.eos.dipalza.model.CondicionVentaDTO;
import cl.eos.dipalza.service.CondicionVentaService;

@RestController
@RequestMapping("/api/condicionventa")
public class CondicionVentaController {

    private final CondicionVentaService condicionVentaService;

    public CondicionVentaController(CondicionVentaService condicionVentaService) {
        this.condicionVentaService = condicionVentaService;
    }

    @GetMapping
    public List<CondicionVentaDTO> getAllConduccion() {
        return condicionVentaService.getAllCondicionVentas();
    }
    
    @GetMapping("/{codigo}")
    public ResponseEntity<CondicionVentaDTO> getRutaById(@PathVariable String codigo) {
    	return condicionVentaService.getCondicionVentaById(codigo)
    			.map(ResponseEntity::ok)
    			.orElse(ResponseEntity.notFound().build());
    }
}
