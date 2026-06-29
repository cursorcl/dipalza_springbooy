package cl.eos.dipalza.controller;

import cl.eos.dipalza.model.NumeradoDTO;
import cl.eos.dipalza.model.NumeradoResumenDTO;
import cl.eos.dipalza.service.NumeradosService;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/numerados")
public class NumeradosController {

    private final NumeradosService  numeradosService;

    public NumeradosController(NumeradosService numeradosService) {
        this.numeradosService = numeradosService;
    }

    @GetMapping
    public List<NumeradoDTO> getAllNumerados() {
        return numeradosService.findAll();
    }


    @GetMapping("/byProduct")
    public List<NumeradoDTO> getNumeradosByCodigoProducto(@RequestParam("codigoProducto") String codigoProducto) {

        return this.numeradosService.findByProducto(codigoProducto);
    }

    @GetMapping("/resumen")
    public List<NumeradoResumenDTO> getGroupedNumerados() {
        return this.numeradosService.findGrouped();
    }

    @GetMapping("/estados")
    public List<NumeradoDTO> getNumeradosByEstado(@Param("estado") String estado) {
        return numeradosService.findAllByEstado(estado);
    }

    @PostMapping
    public NumeradoDTO createNumerado(@RequestBody NumeradoDTO n) {
        return this.numeradosService.save(n);
    }
    @PutMapping
    public NumeradoDTO updateNumerado(@RequestBody NumeradoDTO n) {
        return this.numeradosService.save(n);
    }

    @DeleteMapping
    public void deleteNumerado(@RequestBody NumeradoDTO n) {
        this.numeradosService.deleteById(n.getId());
    }

    @GetMapping("/pesopromedio/{articulo}")
    public Float findPesoPromedioArticulo(@PathVariable String articulo) {
        return numeradosService.findPrecioPromedioArticulo(articulo);
    }
}
