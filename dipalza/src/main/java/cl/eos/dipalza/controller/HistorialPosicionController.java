package cl.eos.dipalza.controller;

import cl.eos.dipalza.entity.HistorialPosicion;
import cl.eos.dipalza.mapper.PosicionMapper;
import cl.eos.dipalza.model.HistorialPosicionDTO;
import cl.eos.dipalza.model.HistoricalPosicionRequest;
import cl.eos.dipalza.model.HistoricalPosicionxVendedorRequest;
import cl.eos.dipalza.service.PosicionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/historial")
public class HistorialPosicionController {

    private final PosicionService posicionService;

    public HistorialPosicionController(PosicionService posicionService) {
        this.posicionService = posicionService;
    }

    @GetMapping("/{vendedorId}")
    public ResponseEntity<Page<HistorialPosicionDTO>> obtenerHistorialPosicionesVendedor(@PathVariable String vendedorId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        Page<HistorialPosicion> entidades = posicionService.obtenerHistorialPosicionesVendedor(vendedorId, page, size);
        Page<HistorialPosicionDTO> dtoPage = entidades.map(PosicionMapper::toHistorialDTO);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping
    public ResponseEntity<Page<HistorialPosicionDTO>> obtenerHistorialPosiciones(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        Page<HistorialPosicion> entidades = posicionService.obtenerHistorialPosiciones( page, size);
        Page<HistorialPosicionDTO> dtoPage = entidades.map(PosicionMapper::toHistorialDTO);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/vendedorrango")
    public ResponseEntity<Page<HistorialPosicionDTO>> obtenerHistorialPosicionesVendedorEntre(HistoricalPosicionxVendedorRequest request) {
        Page<HistorialPosicion> entidades = posicionService.obtenerHistorialPosicionesVendedorEntre(request.vendedorId(), request.fechaInicio(), request.fechaTermino(), request.page(), request.size());
        Page<HistorialPosicionDTO> dtoPage = entidades.map(PosicionMapper::toHistorialDTO);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/rango")
    public ResponseEntity<Page<HistorialPosicionDTO>> obtenerHistorialPosicionesEntre(HistoricalPosicionRequest request) {
        Page<HistorialPosicion> entidades = posicionService.obtenerHistorialPosicionesEntre( request.fechaInicio(), request.fechaTermino(), request.page(), request.size());
        Page<HistorialPosicionDTO> dtoPage = entidades.map(PosicionMapper::toHistorialDTO);
        return ResponseEntity.ok(dtoPage);
    }

}
