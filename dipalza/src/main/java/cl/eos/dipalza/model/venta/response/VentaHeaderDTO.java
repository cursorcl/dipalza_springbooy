package cl.eos.dipalza.model.venta.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VentaHeaderDTO {
	private Long id;
	private LocalDate fecha;

	// Cliente
	private String clienteRut;
	private String clienteCodigo;
	private String clienteNombre; // razon/nombre según tu entidad

	// Vendedor
	private String vendedorCodigo;
	private String vendedorTipo;
	private String vendedorNombre;

	// Ruta
	private String rutaCodigo;
	private String rutaNombre;
	
	private String condicionVentaCodigo;
	private String condicionVentaNombre;

	// Totales del header
	private BigDecimal total;
	private BigDecimal totalIva;
	private BigDecimal totalIla;
	private BigDecimal totalDescuento;

	
	public VentaHeaderDTO() {
		
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

	public String getClienteRut() {
		return clienteRut;
	}

	public void setClienteRut(String clienteRut) {
		this.clienteRut = clienteRut;
	}

	public String getClienteCodigo() {
		return clienteCodigo;
	}

	public void setClienteCodigo(String clienteCodigo) {
		this.clienteCodigo = clienteCodigo;
	}

	public String getClienteNombre() {
		return clienteNombre;
	}

	public void setClienteNombre(String clienteNombre) {
		this.clienteNombre = clienteNombre;
	}

	public String getVendedorCodigo() {
		return vendedorCodigo;
	}

	public void setVendedorCodigo(String vendedorCodigo) {
		this.vendedorCodigo = vendedorCodigo;
	}

	public String getVendedorTipo() {
		return vendedorTipo;
	}

	public void setVendedorTipo(String vendedorTipo) {
		this.vendedorTipo = vendedorTipo;
	}

	public String getRutaCodigo() {
		return rutaCodigo;
	}

	public void setRutaCodigo(String rutaCodigo) {
		this.rutaCodigo = rutaCodigo;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
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

	public BigDecimal getTotalDescuento() {
		return totalDescuento;
	}

	public void setTotalDescuento(BigDecimal totalDescuento) {
		this.totalDescuento = totalDescuento;
	}

	public String getVendedorNombre() {
		return vendedorNombre;
	}

	public void setVendedorNombre(String vendedorNombre) {
		this.vendedorNombre = vendedorNombre;
	}

	public String getRutaNombre() {
		return rutaNombre;
	}

	public void setRutaNombre(String rutaNombre) {
		this.rutaNombre = rutaNombre;
	}

	public String getCondicionVentaCodigo() {
		return condicionVentaCodigo;
	}

	public void setCondicionVentaCodigo(String condicionVentaCodigo) {
		this.condicionVentaCodigo = condicionVentaCodigo;
	}

	public String getCondicionVentaNombre() {
		return condicionVentaNombre;
	}

	public void setCondicionVentaNombre(String condicionVentaNombre) {
		this.condicionVentaNombre = condicionVentaNombre;
	}
	
	

}
