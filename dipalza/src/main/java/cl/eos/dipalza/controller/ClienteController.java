package cl.eos.dipalza.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.eos.dipalza.model.ClienteDTO;
import cl.eos.dipalza.service.ClienteService;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @GetMapping
    public List<ClienteDTO> getAllClientes() {
        return clienteService.getAllClientes();
    }
    
    @GetMapping("/ruta/{ruta}")
    public List<ClienteDTO> getClientesByRuta(@PathVariable String ruta) {
        return clienteService.getClientesByRuta(ruta);
    }

    @GetMapping("/{rut}")
    public ResponseEntity<ClienteDTO> getClienteById(@PathVariable String rut, @RequestParam(required = false)  String codigo) {
        // Si el código no viene o viene vacío, asignar el valor por defecto
        if (codigo == null || codigo.isBlank()) {
            codigo = "   "; // 3 espacios, valor válido en tu base de datos
        }
        return clienteService.getClienteById(rut, codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    


    @PostMapping
    public ClienteDTO createCliente(@RequestBody ClienteDTO clienteDTO) {
        return clienteService.createOrUpdateCliente(clienteDTO);
    }

    @PutMapping("/{rut}/{codigo}")
    public ResponseEntity<ClienteDTO> updateCliente(
            @PathVariable String rut,
            @PathVariable String codigo,
            @RequestBody ClienteDTO clienteDetailsDTO) {
        
        return clienteService.getClienteById(rut, codigo)
                .map(existingClienteDTO -> {
                    // Aseguramos que el ID del DTO a guardar sea el correcto
                    clienteDetailsDTO.setRut(existingClienteDTO.getRut());
                    clienteDetailsDTO.setCodigo(existingClienteDTO.getCodigo());
                    
                    ClienteDTO updatedCliente = clienteService.createOrUpdateCliente(clienteDetailsDTO);
                    return ResponseEntity.ok(updatedCliente);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{rut}/{codigo}")
    public ResponseEntity<Void> deleteCliente(@PathVariable String rut, @PathVariable String codigo) {
        if (clienteService.deleteCliente(rut, codigo)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
