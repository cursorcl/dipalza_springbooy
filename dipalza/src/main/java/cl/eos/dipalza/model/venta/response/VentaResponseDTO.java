// file: cl/eos/dipalza/model/venta/VentaResponseDTO.java
package cl.eos.dipalza.model.venta.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import cl.eos.dipalza.model.venta.reference.ClienteRefDTO;
import cl.eos.dipalza.model.venta.reference.VendedorRefDTO;

public class VentaResponseDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private LocalDate fecha;
	private ClienteRefDTO cliente;
	private VendedorRefDTO vendedor;
	private String condicionVenta;
	private String nombreCondicionVenta;
	// totales calculados cabecera
	private BigDecimal totalNeto;
	private BigDecimal totalDescuento;
	private BigDecimal totalIva;
	private BigDecimal totalIla;
	private BigDecimal total;

	private List<VentaDetalleResponseDTO> detalles;

	public VentaResponseDTO() {
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

	public ClienteRefDTO getCliente() {
		return cliente;
	}

	public void setCliente(ClienteRefDTO cliente) {
		this.cliente = cliente;
	}

	public VendedorRefDTO getVendedor() {
		return vendedor;
	}

	public void setVendedor(VendedorRefDTO vendedor) {
		this.vendedor = vendedor;
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

	public List<VentaDetalleResponseDTO> getDetalles() {
		return detalles;
	}

	public void setDetalles(List<VentaDetalleResponseDTO> detalles) {
		this.detalles = detalles;
	}

	public String getCondicionVenta() {
		return condicionVenta;
	}

	public void setCondicionVenta(String condicionVenta) {
		this.condicionVenta = condicionVenta;
	}

	public String getNombreCondicionVenta() {
		return nombreCondicionVenta;
	}

	public void setNombreCondicionVenta(String nombreCondicionVenta) {
		this.nombreCondicionVenta = nombreCondicionVenta;
	}
	
	
}
