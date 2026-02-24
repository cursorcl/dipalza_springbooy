package cl.eos.dipalza.controller;

import cl.eos.dipalza.entity.Posicion;
import cl.eos.dipalza.mapper.PosicionMapper;
import cl.eos.dipalza.model.PosicionDTO;
import cl.eos.dipalza.service.PosicionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posicion")
public class PosicionController {

    private final PosicionService posicionService;

    public PosicionController(PosicionService posicionService) {
        this.posicionService = posicionService;
    }

    @GetMapping
    public ResponseEntity<List<PosicionDTO>> obtenerPosiciones() {
        List<Posicion> posiciones = posicionService.obtenerPosiciones();
        var posicionesDTO = posiciones.stream().map(PosicionMapper::toPosicionDTO).toList();
        return ResponseEntity.ok(posicionesDTO);
    }

    public ResponseEntity<PosicionDTO> obtenerPosicionPorVendedor(String vendedorId) {
        Posicion posicion = posicionService.obtenerPosicionPorVendedor(vendedorId);
        return ResponseEntity.ok(PosicionMapper.toPosicionDTO(posicion));
    }

    @PostMapping
    public ResponseEntity<Void> registrarPosicion(@RequestBody PosicionDTO dto) {
        posicionService.registrarUbicacion(dto);
        return ResponseEntity.accepted().build();
    }



}
