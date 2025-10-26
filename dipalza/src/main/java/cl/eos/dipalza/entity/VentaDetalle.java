package cl.eos.dipalza.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import cl.eos.dipalza.entity.ids.VentaDetalleId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "venta_detalle")
public class VentaDetalle {

	@EmbeddedId
	private VentaDetalleId id = new VentaDetalleId();

	@MapsId("ventaId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "venta_id", nullable = false)
	private Venta venta;

	@Column(name = "linea", nullable = false, insertable = false, updatable = false)
	private Integer linea;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "producto_id", nullable = false)
	private Producto producto;

	@Column(name = "cantidad", precision = 18, scale = 4, nullable = false)
	private BigDecimal cantidad;

	@Column(name = "precio_unitario", precision = 18, scale = 6, nullable = false)
	private BigDecimal precioUnitario;

	@Column(name = "porc_descuento", precision = 18, scale = 2, nullable = false)
	private BigDecimal porcentajeDescuento;

	@Column(name = "total_descuento", precision = 18, scale = 2, nullable = false)
	private BigDecimal totalDescuento;

	@Column(name = "porc_ila", precision = 18, scale = 2, nullable = false)
	private BigDecimal porcentajeIla;

	@Column(name = "total_ila", precision = 18, scale = 2, nullable = false)
	private BigDecimal totalIla;

	@Column(name = "porc_iva", precision = 18, scale = 2, nullable = false)
	private BigDecimal porcentajeIva;

	@Column(name = "total_iva", precision = 18, scale = 2, nullable = false)
	private BigDecimal totalIva;

	@Column(name = "total_linea", precision = 18, scale = 2, nullable = false)
	private BigDecimal totalLinea;

	@Column(nullable = false)
	private Integer piezas = 0;
	
	@Column(name = "unidad")
	private String unidad;

	@OneToMany(mappedBy = "ventaDetalle", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<VentaDetallePieza> piezasUsadas = new ArrayList<>();

	public void setVenta(Venta venta) {
		this.venta = venta;
		this.id.setVentaId(venta.getId());
	}

	public void setLinea(int linea) {
		this.linea = linea;
		this.id.setLinea(linea);
	}

	public VentaDetalleId getId() {
		return id;
	}

	public void setId(VentaDetalleId id) {
		this.id = id;
	}

	public Integer getLinea() {
		return linea;
	}

	public void setLinea(Integer linea) {
		this.linea = linea;
	}

	public Producto getProducto() {
		return producto;
	}

	public void setProducto(Producto producto) {
		this.producto = producto;
	}

	public BigDecimal getCantidad() {
		return cantidad;
	}

	public void setCantidad(BigDecimal cantidad) {
		this.cantidad = cantidad;
	}

	public BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}

	public void setPrecioUnitario(BigDecimal precioUnitario) {
		this.precioUnitario = precioUnitario;
	}

	public BigDecimal getTotalLinea() {
		return totalLinea;
	}

	public void setTotalLinea(BigDecimal totalLinea) {
		this.totalLinea = totalLinea;
	}

	public Venta getVenta() {
		return venta;
	}

	public BigDecimal getPorcentajeDescuento() {
		return porcentajeDescuento;
	}

	public void setPorcentajeDescuento(BigDecimal porcentajeDescuento) {
		this.porcentajeDescuento = porcentajeDescuento;
	}

	public BigDecimal getTotalDescuento() {
		return totalDescuento;
	}

	public void setTotalDescuento(BigDecimal totalDescuento) {
		this.totalDescuento = totalDescuento;
	}

	public BigDecimal getPorcentajeIla() {
		return porcentajeIla;
	}

	public void setPorcentajeIla(BigDecimal porcentajeIla) {
		this.porcentajeIla = porcentajeIla;
	}

	public BigDecimal getTotalIla() {
		return totalIla;
	}

	public void setTotalIla(BigDecimal totalIla) {
		this.totalIla = totalIla;
	}

	public BigDecimal getPorcentajeIva() {
		return porcentajeIva;
	}

	public void setPorcentajeIva(BigDecimal porcentajeIva) {
		this.porcentajeIva = porcentajeIva;
	}

	public BigDecimal getTotalIva() {
		return totalIva;
	}

	public void setTotalIva(BigDecimal totalIva) {
		this.totalIva = totalIva;
	}

	public Integer getPiezas() {
		return piezas;
	}

	public void setPiezas(Integer piezas) {
		this.piezas = piezas;
	}

	public List<VentaDetallePieza> getPiezasUsadas() {
		return piezasUsadas;
	}

	public void setPiezasUsadas(List<VentaDetallePieza> piezasUsadas) {
		this.piezasUsadas = piezasUsadas;
	}

	public String getUnidad() {
		return unidad;
	}

	public void setUnidad(String unidad) {
		this.unidad = unidad;
	}

}
