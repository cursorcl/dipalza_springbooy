package cl.eos.dipalza.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import cl.eos.dipalza.entity.Venta;
import cl.eos.dipalza.entity.VentaDetalle;
import cl.eos.dipalza.entity.VentaDetallePieza;
import cl.eos.dipalza.model.venta.ids.VentaDetalleIdDTO;
import cl.eos.dipalza.model.venta.reference.ClienteRefDTO;
import cl.eos.dipalza.model.venta.reference.VendedorRefDTO;
import cl.eos.dipalza.model.venta.response.VentaDetalleHeaderDTO;
import cl.eos.dipalza.model.venta.response.VentaDetallePiezaResponseDTO;
import cl.eos.dipalza.model.venta.response.VentaDetalleResponseDTO;
import cl.eos.dipalza.model.venta.response.VentaHeaderDTO;
import cl.eos.dipalza.model.venta.response.VentaResponseDTO;

public class VentaMapper {

	public static VentaResponseDTO toResponseDTO(Venta venta) {
		VentaResponseDTO dto = new VentaResponseDTO();
		dto.setId(venta.getId());
		dto.setFecha(venta.getFecha());

		// Las referencias
		ClienteRefDTO cliente = new ClienteRefDTO();
		cliente.setRut(venta.getCliente().getId().getRut());
		cliente.setCodigo(venta.getCliente().getId().getCodigo());
		cliente.setNombre(venta.getCliente().getRazon());
		dto.setCliente(cliente);

		VendedorRefDTO vendedor = new VendedorRefDTO();
		vendedor.setCodigo(venta.getVendedor().getId().getCodigo());
		vendedor.setTipo(venta.getVendedor().getId().getTipo());
		dto.setVendedor(vendedor);
		
		dto.setCondicionVenta(venta.getCondicionVenta().getCodigo());
		dto.setNombreCondicionVenta(venta.getCondicionVenta().getDescripcion());

		// Totales (asumiendo getters en entity)
		dto.setTotalDescuento(nvl(venta.getTotalDescuento()));
		dto.setTotalIva(nvl(venta.getTotalIva()));
		dto.setTotalIla(nvl(venta.getTotalIla()));
		dto.setTotal(nvl(venta.getTotal()));

		// Detalles
		if (venta.getDetalles() != null) {
			List<VentaDetalleResponseDTO> dets = venta.getDetalles().stream()
					.sorted(Comparator.comparingInt(VentaDetalle::getLinea)).map(VentaMapper::toDetalleResponseDTO)
					.collect(Collectors.toList());
			dto.setDetalles(dets);
		} else {
			dto.setDetalles(List.of());
		}
		return dto;
	}
	
	public static VentaHeaderDTO toHeaderDTO(Venta venta) {
		VentaHeaderDTO dto = new VentaHeaderDTO();
		dto.setId(venta.getId());
		dto.setFecha(venta.getFecha());

		// Las referencias
		dto.setClienteRut(venta.getCliente().getId().getRut());
		dto.setClienteCodigo(venta.getCliente().getId().getCodigo());
		dto.setClienteNombre(venta.getCliente().getRazon());

		dto.setVendedorCodigo(venta.getVendedor().getId().getCodigo());
		dto.setVendedorTipo(venta.getVendedor().getId().getTipo());
		dto.setVendedorNombre(venta.getVendedor().getNombre());
		
		dto.setCondicionVentaCodigo(venta.getCondicionVenta().getCodigo());
		dto.setCondicionVentaNombre(venta.getCondicionVenta().getDescripcion());
		
		dto.setRutaCodigo(venta.getRuta().getCodigo());
		dto.setRutaNombre(venta.getRuta().getDescripcion());

		// Totales (asumiendo getters en entity)
		dto.setTotalDescuento(nvl(venta.getTotalDescuento()));
		dto.setTotalIva(nvl(venta.getTotalIva()));
		dto.setTotalIla(nvl(venta.getTotalIla()));
		dto.setTotal(nvl(venta.getTotal()));

		return dto;
	}

	public static VentaDetalleResponseDTO toDetalleResponseDTO(VentaDetalle d) {

		VentaDetalleResponseDTO dto = new VentaDetalleResponseDTO();

		VentaDetalleIdDTO ventaDetalleDTOId = new VentaDetalleIdDTO(d.getVenta().getId(), d.getLinea());
		dto.setId(ventaDetalleDTOId);
		dto.setProductoId(d.getProducto().getArticulo());
		dto.setDescripcion(d.getProducto().getDescripcion());
		dto.setCantidad(nvl(d.getCantidad()));
		dto.setPrecioUnitario(nvl(d.getPrecioUnitario()));
		dto.setPorcDescuento(nvl(d.getPorcentajeDescuento()));
		dto.setPorcIva(nvl(d.getPorcentajeIva()));
		dto.setPorcIla(nvl(d.getPorcentajeIla()));

		// Calculados de línea
		dto.setNeto(nvl(d.getTotalLinea()));
		dto.setDescuento(nvl(d.getTotalDescuento()));
		dto.setIva(nvl(d.getTotalIva()));
		dto.setIla(nvl(d.getTotalIla()));
		dto.setTotalLinea(nvl(d.getTotalLinea()));

		dto.setPiezas(d.getPiezas());

		// Mapear las piezas numeradas asociadas a este detalle
		if (d.getPiezasUsadas() != null && !d.getPiezasUsadas().isEmpty()) {
			List<VentaDetallePiezaResponseDTO> piezasDto = d.getPiezasUsadas().stream()
					.map(VentaMapper::toPiezaResponseDTO) // Mapeamos cada pieza numerada
					.collect(Collectors.toList());
			dto.setPiezasDetalle(piezasDto); // Establecemos las piezas en el DTO
		} else {
			dto.setPiezasDetalle(List.of()); // Si no hay piezas, asignamos una lista vacía
		}

		return dto;
	}
	
	public static VentaDetalleHeaderDTO toVentaDetalleHeaderResponseDTO(VentaDetalle d) {

		VentaDetalleHeaderDTO dto = new VentaDetalleHeaderDTO();

		dto.setVentaId(d.getVenta().getId());
		dto.setLinea(d.getLinea());
		dto.setProductoId(d.getProducto().getArticulo());
		dto.setNombreProducto(d.getProducto().getDescripcion());
		dto.setCantidad(nvl(d.getCantidad()));
		dto.setPrecioUnitario(nvl(d.getPrecioUnitario()));
		dto.setPorcDescuento(nvl(d.getPorcentajeDescuento()));
		dto.setPorcIva(nvl(d.getPorcentajeIva()));
		dto.setPorcIla(nvl(d.getPorcentajeIla()));

		// Calculados de línea
		dto.setNeto(nvl(d.getTotalLinea()));
		dto.setDescuento(nvl(d.getTotalDescuento()));
		dto.setIva(nvl(d.getTotalIva()));
		dto.setIla(nvl(d.getTotalIla()));
		dto.setTotalLinea(nvl(d.getTotalLinea()));

		dto.setPiezas(d.getPiezas());
		
		dto.setUnidad(d.getUnidad());

		return dto;
	}

	public static VentaDetallePiezaResponseDTO toPiezaResponseDTO(VentaDetallePieza pieza) {
		VentaDetallePiezaResponseDTO piezaDto = new VentaDetallePiezaResponseDTO();
		piezaDto.setId(pieza.getId()); // Asumimos que invId es el identificador de la pieza
		piezaDto.setPeso(nvl(pieza.getPeso())); // El peso es opcional
		piezaDto.setCreadoEn(pieza.getCreadoEn() != null ? pieza.getCreadoEn() : LocalDate.now());

		return piezaDto;
	}

	private static BigDecimal nvl(BigDecimal x) {
		return x == null ? BigDecimal.ZERO : x;
	}
}
