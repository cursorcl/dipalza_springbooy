package cl.eos.dipalza.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.eos.dipalza.model.IlaDTO;
import cl.eos.dipalza.service.IlaService;

@RestController
@RequestMapping("/api/ila")
public class IlaController {

    private final IlaService ilaService;

    public IlaController(IlaService conduccionService) {
        this.ilaService = conduccionService;
    }

    @GetMapping
    public List<IlaDTO> getAllIla() {
        return ilaService.findAllByOrderByDescripcionAsc();
    }
    
    @GetMapping("/{codigo}")
    public ResponseEntity<IlaDTO> getIlaById(@PathVariable String codigo) {
    	return ilaService.getIlaById(codigo)
    			.map(ResponseEntity::ok)
    			.orElse(ResponseEntity.notFound().build());
    }
}
