// file: cl/eos/dipalza/model/venta/VentaDetalleResponseDTO.java
package cl.eos.dipalza.model.venta.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import cl.eos.dipalza.model.venta.ids.VentaDetalleIdDTO;

public class VentaDetalleResponseDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private VentaDetalleIdDTO id;
	private String productoId;
	private String descripcion;
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

	private Integer piezas;
	private List<VentaDetallePiezaResponseDTO> piezasDetalle;

	public VentaDetalleResponseDTO() {
	}

	public VentaDetalleIdDTO getId() {
		return id;
	}

	public void setId(VentaDetalleIdDTO id) {
		this.id = id;
	}

	public String getProductoId() {
		return productoId;
	}

	public void setProductoId(String productoId) {
		this.productoId = productoId;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
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

	public List<VentaDetallePiezaResponseDTO> getPiezasDetalle() {
		return piezasDetalle;
	}

	public void setPiezasDetalle(List<VentaDetallePiezaResponseDTO> piezasDetalle) {
		this.piezasDetalle = piezasDetalle;
	}
}
