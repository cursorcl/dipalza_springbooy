package cl.eos.dipalza.model.venta.response;

import java.math.BigDecimal;

public class VentaDetalleHeaderDTO {
	private Long ventaId;
	private Integer linea;
	private String productoId;
	private String nombreProducto;
	private BigDecimal cantidad;
	private BigDecimal precioUnitario;
	private BigDecimal porcDescuento;
	private BigDecimal porcIva;
	private BigDecimal porcIla;

	// calculados línea
	private BigDecimal neto;
	private BigDecimal descuento;
	private BigDecimal iva;
	private BigDecimal ila;
	private BigDecimal totalLinea;
	
	private String unidad;

	private Integer piezas;

	public Long getVentaId() {
		return ventaId;
	}

	public void setVentaId(Long ventaId) {
		this.ventaId = ventaId;
	}

	public Integer getLinea() {
		return this.linea;
	}
	
	public void setLinea(Integer linea) {
		this.linea = linea;
	}
	
	public String getProductoId() {
		return productoId;
	}

	public void setProductoId(String productoId) {
		this.productoId = productoId;
	}

	public String getNombreProducto() {
		return nombreProducto;
	}

	public void setNombreProducto(String descripcion) {
		this.nombreProducto = descripcion;
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

	public BigDecimal getPorcDescuento() {
		return porcDescuento;
	}

	public void setPorcDescuento(BigDecimal porcDescuento) {
		this.porcDescuento = porcDescuento;
	}

	public BigDecimal getPorcIva() {
		return porcIva;
	}

	public void setPorcIva(BigDecimal porcIva) {
		this.porcIva = porcIva;
	}

	public BigDecimal getPorcIla() {
		return porcIla;
	}

	public void setPorcIla(BigDecimal porcIla) {
		this.porcIla = porcIla;
	}

	public BigDecimal getNeto() {
		return neto;
	}

	public void setNeto(BigDecimal neto) {
		this.neto = neto;
	}

	public BigDecimal getDescuento() {
		return descuento;
	}

	public void setDescuento(BigDecimal descuento) {
		this.descuento = descuento;
	}

	public BigDecimal getIva() {
		return iva;
	}

	public void setIva(BigDecimal iva) {
		this.iva = iva;
	}

	public BigDecimal getIla() {
		return ila;
	}

	public void setIla(BigDecimal ila) {
		this.ila = ila;
	}

	public BigDecimal getTotalLinea() {
		return totalLinea;
	}

	public void setTotalLinea(BigDecimal totalLinea) {
		this.totalLinea = totalLinea;
	}

	public Integer getPiezas() {
		return piezas;
	}

	public void setPiezas(Integer piezas) {
		this.piezas = piezas;
	}

	public String getUnidad() {
		return unidad;
	}

	public void setUnidad(String unidad) {
		this.unidad = unidad;
	}
	   
	
}
