package cl.eos.dipalza.mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import cl.eos.dipalza.entity.Cliente;
import cl.eos.dipalza.entity.Producto;
import cl.eos.dipalza.entity.Ruta;
import cl.eos.dipalza.entity.Vendedor;
import cl.eos.dipalza.entity.Venta;
import cl.eos.dipalza.entity.VentaDetalle;
import cl.eos.dipalza.entity.VentaDetallePieza;
import cl.eos.dipalza.entity.ids.ClienteId;
import cl.eos.dipalza.entity.ids.VendedorId;
import cl.eos.dipalza.exceptions.MissingDataException;
import cl.eos.dipalza.model.venta.crear.VentaCreateDTO;
import cl.eos.dipalza.model.venta.crear.VentaDetalleCreateDTO;
import cl.eos.dipalza.model.venta.crear.VentaDetallePiezaCreateDTO;

public class VentaMapperCreation {

	// Mapeo de VentaCreateDTO a Venta
	public static Venta toVentaEntity(VentaCreateDTO dto) {

		if (dto.getCodigoCliente() == null)
			throw new MissingDataException("Falta el código de cliente!");
		if (dto.getRutCliente() == null || dto.getRutCliente().isBlank())
			throw new MissingDataException("Falta el rut del cliente!");

		if (dto.getCodigoVendedor() == null || dto.getCodigoVendedor().isBlank())
			throw new MissingDataException("Falta el código de vendedor!");

		if (dto.getTipoVendedor() == null || dto.getTipoVendedor().isBlank())
			throw new MissingDataException("Falta el tipo de vendedor!");

		if (dto.getCodigoRuta() == null || dto.getCodigoRuta().isBlank())
			throw new MissingDataException("Falta el código de ruta!");

		Cliente cliente = new Cliente();
		cliente.setId(new ClienteId(dto.getRutCliente(), dto.getCodigoCliente()));

		Vendedor vendedor = new Vendedor();
		vendedor.setId(new VendedorId(dto.getCodigoVendedor(), dto.getTipoVendedor()));

		Ruta ruta = new Ruta();
		ruta.setCodigo(dto.getCodigoRuta());
		

		Venta venta = new Venta();
		// Mapear propiedades de la cabecera de la venta
		venta.setFecha(dto.getFecha());
		venta.setCliente(cliente);
		venta.setVendedor(vendedor);
		venta.setRuta(ruta);

		// Mapear detalles (líneas de venta)
        // Mapear los detalles de la venta
        if (dto.getDetalles() != null) {
            List<VentaDetalle> detalles = dto.getDetalles().stream()
                    .map((VentaDetalleCreateDTO detalleDto) -> toVentaDetalleEntity(detalleDto, venta))
                    .collect(Collectors.toList());
            venta.setDetalles(detalles);
        }

		return venta;
	}

	// Mapeo de VentaDetalleCreateDTO a VentaDetalle
	public static VentaDetalle toVentaDetalleEntity(VentaDetalleCreateDTO dto, Venta venta) {
		VentaDetalle detalle = new VentaDetalle();
		
		Producto producto = new Producto();
		producto.setArticulo(dto.getProductoId());
		detalle.setProducto(producto);
		
		
		detalle.setCantidad(dto.getCantidad());
		detalle.setPrecioUnitario(dto.getPrecioUnitario());
		detalle.setPorcentajeDescuento(dto.getPorcDescuento());
		detalle.setPorcentajeIva(dto.getPorcIva());
		detalle.setPorcentajeIla(dto.getPorcIla());
		detalle.setPiezas(dto.getPiezas());

		// Mapear piezas numeradas si vienen en el DTO
		if (dto.getPiezasDetalle() != null && !dto.getPiezasDetalle().isEmpty()) {
			List<VentaDetallePieza> piezas = dto.getPiezasDetalle().stream()
					.map(VentaMapperCreation::toVentaDetallePiezaEntity).toList();
			detalle.setPiezasUsadas(piezas);
		}
		
		return detalle;
	}

	// Mapeo de VentaDetallePiezaCreateDTO a VentaDetallePieza
	public static VentaDetallePieza toVentaDetallePiezaEntity(VentaDetallePiezaCreateDTO dto) {
		VentaDetallePieza pieza = new VentaDetallePieza();

		pieza.setId(dto.getId()); // invId debe ser único para cada pieza
		pieza.setPeso(dto.getPeso());
		pieza.setCreadoEn(LocalDate.now());

		return pieza;
	}

}
