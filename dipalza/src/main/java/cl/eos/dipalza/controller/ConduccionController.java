package cl.eos.dipalza.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.eos.dipalza.model.ConduccionDTO;
import cl.eos.dipalza.service.ConduccionService;

@RestController
@RequestMapping("/api/conduccion")
public class ConduccionController {

    private final ConduccionService conduccionService;

    public ConduccionController(ConduccionService conduccionService) {
        this.conduccionService = conduccionService;
    }

    @GetMapping
    public List<ConduccionDTO> getAllConduccion() {
        return conduccionService.getAllConduccions();
    }
    
    @GetMapping("/{codigo}")
    public ResponseEntity<ConduccionDTO> getConduccionById(@PathVariable String codigo) {
    	return conduccionService.getConduccionById(codigo)
    			.map(ResponseEntity::ok)
    			.orElse(ResponseEntity.notFound().build());
    }
}
