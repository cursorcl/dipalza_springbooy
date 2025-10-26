package cl.eos.dipalza.model.venta.modificar;

import java.math.BigDecimal;
import java.util.List;

public class VentaDetalleUpdateDTO {

	public Long ventaId;
	public Integer linea;
	public String productoId;
	public BigDecimal cantidad;
	public BigDecimal precioUnitario;
	public BigDecimal porcentajeDescuento;
	public BigDecimal totalDescuento;
	public BigDecimal porcentajeIla;
	public BigDecimal totalIla;
	public BigDecimal porcentajeIva;
	public BigDecimal totalIva;
	public BigDecimal totalLinea;
	public Integer piezas;
	public List<VentaDetallePiezaUpdateDTO> piezasDetalle;

	
	
	public Long getVentaId() {
		return ventaId;
	}

	public void setVentaId(Long ventaId) {
		this.ventaId = ventaId;
	}

	public Integer getLinea() {
		return linea;
	}

	public void setLinea(Integer linea) {
		this.linea = linea;
	}

	public String getProductoId() {
		return productoId;
	}

	public void setPorductoId(String productoId) {
		this.productoId = productoId;
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

	public List<VentaDetallePiezaUpdateDTO> getPiezasDetalle() {
		return piezasDetalle;
	}

	public void setPiezasDetalle(List<VentaDetallePiezaUpdateDTO> piezasDetalle) {
		this.piezasDetalle = piezasDetalle;
	}

}
