package cl.eos.dipalza.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import cl.eos.dipalza.entity.Cliente;
import cl.eos.dipalza.entity.Numerados;
import cl.eos.dipalza.entity.Ruta;
import cl.eos.dipalza.entity.Vendedor;
import cl.eos.dipalza.entity.Venta;
import cl.eos.dipalza.entity.VentaDetalle;
import cl.eos.dipalza.entity.VentaDetallePieza;
import cl.eos.dipalza.entity.ids.ClienteId;
import cl.eos.dipalza.entity.ids.VendedorId;
import cl.eos.dipalza.mapper.VentaMapper;
import cl.eos.dipalza.model.venta.VentaDTO;
import cl.eos.dipalza.model.venta.VentaDetalleDTO;
import cl.eos.dipalza.model.venta.VentaDetallePiezaDTO;
import cl.eos.dipalza.repository.ProductoRepository;
import cl.eos.dipalza.repository.VentaDetallePiezaRepository;
import cl.eos.dipalza.repository.VentaDetalleRepository;
import cl.eos.dipalza.repository.VentaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class VentaService {

	// inyección gestionada por Spring
	@PersistenceContext
	private EntityManager em;

	private final VentaRepository ventaRepository;
	private final VentaDetalleRepository ventaDetalleRepository;
	private final VentaDetallePiezaRepository ventaDetallePiezaRepository;

	public VentaService(VentaRepository ventaRepository, VentaDetalleRepository ventaDetalleRepository,
			VentaDetallePiezaRepository ventaDetallePiezaRepository, ProductoRepository productoRepository) {
		this.ventaRepository = ventaRepository;
		this.ventaDetalleRepository = ventaDetalleRepository;
		this.ventaDetallePiezaRepository = ventaDetallePiezaRepository;
	}

	public VentaDTO crearVenta(VentaDTO dto) {

		// Usando el mapper para convertir DTO a entidad
		Venta venta = VentaMapper.toVentaEntity(dto);

		// Guardar la venta en la base de datos (esto asigna un id a la venta)
		venta = ventaRepository.save(venta);

		// En la creación, puede venir sin los registros de venta.
		if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
			// Luego, los detalles de la venta se guardan con el id generado
			for (VentaDetalle detalle : venta.getDetalles()) {
				detalle.setVentaId(venta.getId()); // Establecemos la relación de la venta con cada detalle
			}

			// Guardar los detalles en la base de datos (debe estar relacionado con la
			// venta)
			venta.getDetalles().forEach(detalle -> ventaDetalleRepository.save(detalle));

			// Guardar los detalles en la base de datos (debe estar relacionado con la
			// venta)
			for (VentaDetalle detalle : venta.getDetalles()) {
				// Guardar cada detalle de venta
				ventaDetalleRepository.save(detalle);

				// Guardar las piezas asociadas a ese detalle
				if (detalle.getPiezasUsadas() != null) {
					for (VentaDetallePieza pieza : detalle.getPiezasUsadas()) {
						pieza.setVentaDetalle(detalle); // Asignar la relación entre pieza y detalle
						ventaDetallePiezaRepository.save(pieza); // Guardar la pieza
					}
				}
			}
		}

		// Retornar el DTO de respuesta
		return VentaMapper.toVentaDTO(venta);
	}

	public VentaDTO actualizarVenta(Long id, VentaDTO dto) {
		Objects.requireNonNull(dto, "dto no puede ser null");
		Objects.requireNonNull(id, "id no puede ser null");

		Venta venta = ventaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + id));

		// Cabecera
		if (dto.getFecha() != null)
			venta.setFecha(dto.getFecha());

		if (dto.getRutCliente() != null && dto.getCodigoCliente() != null) {
			ClienteId clienteId = new ClienteId(dto.getRutCliente(), dto.getCodigoCliente());
			Cliente cliente = em.getReference(Cliente.class, clienteId);
			venta.setCliente(cliente);
		}
		if (dto.getCodigoVendedor() != null && dto.getTipoVendedor() != null) {
			VendedorId vendedorId = new VendedorId(dto.getCodigoVendedor(), dto.getTipoVendedor());
			Vendedor vendedor = em.getReference(Vendedor.class, vendedorId);
			venta.setVendedor(vendedor);
		}
		if (dto.getCodigoRuta() != null) {
			Ruta ruta = em.getReference(Ruta.class, dto.getCodigoRuta());
			venta.setRuta(ruta);
		}

		syncDetalles(venta, dto.getDetalles());

		// Recalcular totales cabecera
		recalcularTotalesVenta(venta);

		venta = ventaRepository.save(venta);
		return VentaMapper.toVentaDTO(venta);
	}

	private void syncDetalles(Venta venta, List<VentaDetalleDTO> detallesDto) {
		Map<Long, VentaDetalle> existentes = venta.getDetalles().stream()
				.collect(Collectors.toMap(VentaDetalle::getId, Function.identity()));

		List<VentaDetalle> nuevos = new ArrayList<>();
		if (detallesDto != null) {
			for (VentaDetalleDTO dto : detallesDto) {
				VentaDetalle detalle = existentes.remove(dto.getId());

				if (detalle == null) {
					// Nuevo
					detalle = new VentaDetalle();
					detalle.setVentaId(venta.getId());
				}

				// Campos editables
				detalle.setCantidad(dto.getCantidad());
				detalle.setPrecioUnitario(dto.getPrecioUnitario());
				detalle.setTotalLinea(dto.getTotalLinea());
				detalle.setPorcentajeDescuento(dto.getPorcentajeDescuento());
				detalle.setPorcentajeIla(dto.getPorcentajeIla());
				detalle.setPorcentajeIva(dto.getPorcentajeIva());

				// Sincroniza las piezas
				syncPiezas(detalle, dto.getPiezasDetalle());

				nuevos.add(detalle);
			}

			// Los que quedaron en el mapa no están en el DTO → eliminar
			existentes.values().forEach(venta.getDetalles()::remove);
		}
		// Reemplaza por la nueva lista sincronizada
		venta.getDetalles().clear();
		venta.getDetalles().addAll(nuevos);
	}

	private void syncPiezas(VentaDetalle detalle, List<VentaDetallePiezaDTO> piezasDto) {
		Map<Long, VentaDetallePieza> existentes = detalle.getPiezasUsadas().stream().filter(p -> p.getId() != null)
				.collect(Collectors.toMap(VentaDetallePieza::getId, Function.identity()));

		List<VentaDetallePieza> nuevas = new ArrayList<>();

		for (VentaDetallePiezaDTO dto : piezasDto) {
			VentaDetallePieza pieza = (dto.getId() != null) ? existentes.remove(dto.getId()) : new VentaDetallePieza();

			pieza.setVentaDetalle(detalle);
			pieza.setPeso(dto.getPeso());
			pieza.setCreadoEn(dto.getCreadoEn() != null ? dto.getCreadoEn() : LocalDate.now());

			// Asignar relación con inventario
			Numerados numerado = em.getReference(Numerados.class, dto.getInvId());
			pieza.setNumerado(numerado);

			nuevas.add(pieza);
		}

		// Eliminar las que no vienen más
		existentes.values().forEach(detalle.getPiezasUsadas()::remove);

		detalle.setPiezasUsadas(nuevas);
	}

	public boolean eliminarVenta(Long id) {
		Objects.requireNonNull(id, "id no puede ser null");

		Venta venta = ventaRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + id));

		// Si requiere borrado lógico, marque estado/flag en lugar de delete.
		ventaRepository.delete(venta);

		return true;
	}

	// Obtener todas las ventas de un vendedor en una fecha específica
	public List<VentaDTO> obtenerVentasPorVendedorYFecha(String vendedorCodigo, LocalDate fecha) {
		List<Venta> ventas = ventaRepository.findVentasByVendedorAndFecha(vendedorCodigo, fecha);

		return ventas.stream().map(v -> VentaMapper.toVentaDTO(v)).toList();
	}

	// Obtener la última venta de un cliente
	public VentaDTO obtenerUltimaVentaDeCliente(String rutCliente) {
		Venta ultimaVenta = ventaRepository.findLastVentaByCliente(rutCliente);

		return ultimaVenta == null ? null : VentaMapper.toVentaDTO(ultimaVenta);
	}

	public List<VentaDetalleDTO> obtenerDetallePorVenta(Long ventaId) {
		List<VentaDetalle> detalles = ventaDetalleRepository.findByVentaId(ventaId);
		return detalles.stream().map(d -> VentaMapper.toVentaDetalleDTO(d)).toList();
	}

	private static BigDecimal nvl(BigDecimal x) {
		return x == null ? BigDecimal.ZERO : x;
	}
	// =========================
	// Cálculos de totales
	// =========================

	private void calcularTotalesLinea(VentaDetalle d) {
		BigDecimal cant = nvl(d.getCantidad());
		BigDecimal pu = nvl(d.getPrecioUnitario());
		BigDecimal pDesc = pct(nvl(d.getPorcentajeDescuento()));
		BigDecimal pIva = pct(nvl(d.getPorcentajeIva()));
		BigDecimal pIla = pct(nvl(d.getPorcentajeIla()));

		BigDecimal bruto = pu.multiply(cant); // PU * cant
		BigDecimal desc = bruto.multiply(pDesc); // descuento
		BigDecimal totlaLinea = bruto.subtract(desc); // neto
		BigDecimal ila = totlaLinea.multiply(pIla); // ILA sobre neto (ajuste si su regla difiere)
		BigDecimal iva = totlaLinea.multiply(pIva); // IVA

		d.setTotalDescuento(scale(desc));
		d.setTotalIla(scale(ila));
		d.setTotalIva(scale(iva));
		d.setTotalLinea(scale(totlaLinea));
	}

	private void recalcularTotalesVenta(Venta v) {
		BigDecimal desc = BigDecimal.ZERO;
		BigDecimal iva = BigDecimal.ZERO;
		BigDecimal ila = BigDecimal.ZERO;
		BigDecimal total = BigDecimal.ZERO;

		if (v.getDetalles() != null) {
			for (VentaDetalle d : v.getDetalles()) {
				calcularTotalesLinea(d); // garantizar consistencia
				desc = desc.add(nvl(d.getTotalDescuento()));
				iva = iva.add(nvl(d.getTotalIva()));
				ila = ila.add(nvl(d.getTotalIla()));
				total = total.add(nvl(d.getTotalLinea()));
			}
		}

		// Asuma setters en entity Venta:
		v.setTotalDescuento(scale(desc));
		v.setTotalIva(scale(iva));
		v.setTotalIla(scale(ila));
		v.setTotal(scale(total));
	}

	private static BigDecimal pct(BigDecimal porcentaje0a100) {
		// 19 -> 0.19
		if (porcentaje0a100.compareTo(BigDecimal.ZERO) <= 0)
			return BigDecimal.ZERO;
		return porcentaje0a100.movePointLeft(2);
	}

	@SuppressWarnings("deprecation")
	private static BigDecimal scale(BigDecimal x) {
		// Ajuste a la escala que maneje su BD (p.ej., money 4 decimales)
		return x.setScale(4, BigDecimal.ROUND_HALF_UP);
	}

	// ===============================
	// DETALLES
	// ===============================
	public Venta eliminarItemVenta(Long id) {
		Objects.requireNonNull(id, "id no puede ser null");

		VentaDetalle ventaDetalle = ventaDetalleRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Item de Venta no encontrado: " + id));

		Venta venta = ventaDetalle.getVenta();

		venta.removeDetalle(ventaDetalle);
		recalcularTotalesVenta(venta);

		ventaRepository.save(venta);

		return venta;
	}
	
	public Venta grabarVentaDetalle(VentaDetalleDTO ventaDetalleDTO) {
		Objects.requireNonNull(ventaDetalleDTO, "ventaDetalle no puede ser null");
		Objects.requireNonNull(ventaDetalleDTO.getVentaId(), "El Identificador de la venta no puede ser null");
		
		Venta venta = ventaRepository.findById(ventaDetalleDTO.getVentaId()).orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + ventaDetalleDTO.getVentaId()));

		VentaDetalle  ventaDetalle = VentaMapper.toVentaDetalleEntity(ventaDetalleDTO, venta.getId());
			
		// Es nueva
		if(ventaDetalleDTO.getId() == null || ventaDetalleDTO.getId().longValue() == -1)
		{
			venta.getDetalles().add(ventaDetalle);
			venta = ventaRepository.save(venta);
			return venta;
		}
			
		VentaDetalle oldVentaDetalle = ventaDetalleRepository.findById(ventaDetalleDTO.getId())
				.orElseThrow(() -> new EntityNotFoundException("Item de Venta no encontrado: " + ventaDetalleDTO.getId()));
		venta.getDetalles().replaceAll(d -> Objects.equals(d.getId(), oldVentaDetalle.getId()) ? ventaDetalle : d);
		venta = ventaRepository.save(venta);
		return venta;
		
	}
}
