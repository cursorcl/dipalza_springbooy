package cl.eos.dipalza.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

import cl.eos.dipalza.model.venta.crear.VentaCreateDTO;
import cl.eos.dipalza.model.venta.modificar.VentaUpdateDTO;
import cl.eos.dipalza.model.venta.response.VentaDetalleHeaderDTO;
import cl.eos.dipalza.model.venta.response.VentaHeaderDTO;
import cl.eos.dipalza.model.venta.response.VentaResponseDTO;
import cl.eos.dipalza.service.VentaService;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    
    public VentaController(VentaService ventaService) {
    	this.ventaService = ventaService;
    }
    // Crear una nueva venta
    @PostMapping
    public ResponseEntity<VentaResponseDTO> crearVenta(@RequestBody VentaCreateDTO venta) {
    	VentaResponseDTO nuevaVenta = ventaService.crearVenta(venta);
        return new ResponseEntity<>(nuevaVenta, HttpStatus.CREATED);
    }

    // Modificar una venta existente
    @PutMapping("/{id}")
    public ResponseEntity<VentaResponseDTO> actualizarVenta(@PathVariable Long id, @RequestBody VentaUpdateDTO ventaUpdateDTO) {
    	VentaResponseDTO ventaActualizada = ventaService.actualizarVenta(id, ventaUpdateDTO);
        if (ventaActualizada == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(ventaActualizada, HttpStatus.OK);
    }

    // Eliminar una venta
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarVenta(@PathVariable Long id) {
        boolean eliminado = ventaService.eliminarVenta(id);
        if (!eliminado) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Obtener todas las ventas de un vendedor en una fecha específica
    @GetMapping("/vendedor/{codigo}/fecha")
    public ResponseEntity<List<VentaResponseDTO>> obtenerVentasPorVendedorYFecha(
            @PathVariable String codigo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
    	
        List<VentaResponseDTO> ventas = ventaService.obtenerVentasPorVendedorYFecha(codigo, fecha);
        return new ResponseEntity<>(ventas, HttpStatus.OK);
    }

    
    
    // Obtener la última venta de un cliente
    @GetMapping("/cliente/{rut}")
    public ResponseEntity<VentaResponseDTO> obtenerUltimaVentaDeCliente(@PathVariable String rut) {
    	VentaResponseDTO ultimaVenta = ventaService.obtenerUltimaVentaDeCliente(rut);
        if (ultimaVenta == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(ultimaVenta, HttpStatus.OK);
    }

    // Obtener todas las ventas de un vendedor en una fecha específica
    @GetMapping("/header/vendedor/{codigo}/fecha")
    public ResponseEntity<List<VentaHeaderDTO>> obtenerHeaderVentasPorVendedorYFecha(
            @PathVariable String codigo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
    	
        List<VentaHeaderDTO> ventas = ventaService.obtenerUltimaVentaDeVendendorDia(codigo, fecha);
        return new ResponseEntity<>(ventas, HttpStatus.OK);
    }
    
    @GetMapping("/{idVenta}/detalles")
    public ResponseEntity<List<VentaDetalleHeaderDTO>> obtenerVentaPorDetalles(@PathVariable Long idVenta) {
    	return new ResponseEntity<>(ventaService.obtenerDetallePorVenta(idVenta), HttpStatus.OK);
    }
    
}
