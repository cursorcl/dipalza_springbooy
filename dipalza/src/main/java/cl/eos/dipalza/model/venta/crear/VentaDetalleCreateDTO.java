// file: cl/eos/dipalza/model/venta/VentaDetalleCreateDTO.java
package cl.eos.dipalza.model.venta.crear;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class VentaDetalleCreateDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private String productoId; // obligatorio
	private String descripcion; // opcional
	private BigDecimal cantidad; // > 0
	private BigDecimal precioUnitario; // ≥ 0
	private BigDecimal porcDescuento; // 0..100 (opcional)
	private BigDecimal porcIva; // 0..100 (obligatorio según reglas)
	private BigDecimal porcIla; // 0..100 (opcional)
	private Integer piezas; // default 0
	private Integer nroLinea;
	private List<VentaDetallePiezaCreateDTO> piezasDetalle; // opcional

	public VentaDetalleCreateDTO() {
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

	public Integer getPiezas() {
		return piezas;
	}

	public void setPiezas(Integer piezas) {
		this.piezas = piezas;
	}

	public List<VentaDetallePiezaCreateDTO> getPiezasDetalle() {
		return piezasDetalle;
	}

	public void setPiezasDetalle(List<VentaDetallePiezaCreateDTO> piezasDetalle) {
		this.piezasDetalle = piezasDetalle;
	}

	public Integer getNroLinea() {
		return nroLinea;
	}

	public void setNroLinea(Integer nroLinea) {
		this.nroLinea = nroLinea;
	}
	
	
}
