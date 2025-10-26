// file: cl/eos/dipalza/model/venta/VentaUpdateDTO.java
package cl.eos.dipalza.model.venta.modificar;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public class VentaUpdateDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private LocalDate fecha;
	private BigDecimal totalNeto;
	private BigDecimal totalDescuento;
	private BigDecimal totalIva;
	private BigDecimal totalIla;
	private BigDecimal total;
	private String estado;

	private String rutCliente;
	private String codigoCliente;
	private String codigoVendedor;
	private String tipoVendedor;
	private String codigoRuta;

	private List<VentaDetalleUpdateDTO> detalles;

	public VentaUpdateDTO() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public void setFecha(LocalDate fecha) {
		this.fecha = fecha;
	}

	public BigDecimal getTotalNeto() {
		return totalNeto;
	}

	public void setTotalNeto(BigDecimal totalNeto) {
		this.totalNeto = totalNeto;
	}

	public BigDecimal getTotalDescuento() {
		return totalDescuento;
	}

	public void setTotalDescuento(BigDecimal totalDescuento) {
		this.totalDescuento = totalDescuento;
	}

	public BigDecimal getTotalIva() {
		return totalIva;
	}

	public void setTotalIva(BigDecimal totalIva) {
		this.totalIva = totalIva;
	}

	public BigDecimal getTotalIla() {
		return totalIla;
	}

	public void setTotalIla(BigDecimal totalIla) {
		this.totalIla = totalIla;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public String getRutCliente() {
		return rutCliente;
	}

	public void setRutCliente(String rutCliente) {
		this.rutCliente = rutCliente;
	}

	public String getCodigoCliente() {
		return codigoCliente;
	}

	public void setCodigoCliente(String codigoCliente) {
		this.codigoCliente = codigoCliente;
	}

	public String getCodigoVendedor() {
		return codigoVendedor;
	}

	public void setCodigoVendedor(String codigoVendedor) {
		this.codigoVendedor = codigoVendedor;
	}

	public String getTipoVendedor() {
		return tipoVendedor;
	}

	public void setTipoVendedor(String tipoVendedor) {
		this.tipoVendedor = tipoVendedor;
	}

	public String getCodigoRuta() {
		return codigoRuta;
	}

	public void setCodigoRuta(String codigoRuta) {
		this.codigoRuta = codigoRuta;
	}

	public List<VentaDetalleUpdateDTO> getDetalles() {
		return detalles;
	}

	public void setDetalles(List<VentaDetalleUpdateDTO> detalles) {
		this.detalles = detalles;
	}

}
