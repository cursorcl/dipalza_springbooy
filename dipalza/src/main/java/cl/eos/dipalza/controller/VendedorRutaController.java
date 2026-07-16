package cl.eos.dipalza.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.eos.dipalza.model.RutaDTO;
import cl.eos.dipalza.service.VendedorRutaService;

@RestController
@RequestMapping("/api/vendedores/{codigo}/{tipo}/rutas")
public class VendedorRutaController {

    private final VendedorRutaService vendedorRutaService;

    public VendedorRutaController(VendedorRutaService vendedorRutaService) {
        this.vendedorRutaService = vendedorRutaService;
    }

    @GetMapping
    public List<RutaDTO> getRutas(@PathVariable String codigo, @PathVariable String tipo) {
        return vendedorRutaService.getRutasByVendedor(codigo, tipo);
    }

    @PutMapping
    public List<RutaDTO> setRutas(@PathVariable String codigo, @PathVariable String tipo,
                                   @RequestBody List<String> codigosRuta) {
        return vendedorRutaService.asignarRutas(codigo, tipo, codigosRuta);
    }
}
