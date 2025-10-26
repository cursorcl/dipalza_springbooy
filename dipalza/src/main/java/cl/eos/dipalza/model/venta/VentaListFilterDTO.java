// file: cl/eos/dipalza/model/venta/VentaListFilterDTO.java
package cl.eos.dipalza.model.venta;

import java.io.Serializable;
import java.time.LocalDate;

public class VentaListFilterDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private LocalDate desde; // rango fecha inicio
	private LocalDate hasta; // rango fecha fin
	private String rutCliente;
	private String codigoVendedor;

	private Integer pagina = 0; // ≥ 0
	private Integer tamano = 20; // 1..200
	private String ordenarPor = "fecha"; // fecha | total | cliente | vendedor
	private String direccion = "DESC"; // ASC | DESC

	public VentaListFilterDTO() {
	}

	public LocalDate getDesde() {
		return desde;
	}

	public void setDesde(LocalDate desde) {
		this.desde = desde;
	}

	public LocalDate getHasta() {
		return hasta;
	}

	public void setHasta(LocalDate hasta) {
		this.hasta = hasta;
	}

	public String getRutCliente() {
		return rutCliente;
	}

	public void setRutCliente(String rutCliente) {
		this.rutCliente = rutCliente;
	}

	public String getCodigoVendedor() {
		return codigoVendedor;
	}

	public void setCodigoVendedor(String codigoVendedor) {
		this.codigoVendedor = codigoVendedor;
	}

	public Integer getPagina() {
		return pagina;
	}

	public void setPagina(Integer pagina) {
		this.pagina = pagina;
	}

	public Integer getTamano() {
		return tamano;
	}

	public void setTamano(Integer tamano) {
		this.tamano = tamano;
	}

	public String getOrdenarPor() {
		return ordenarPor;
	}

	public void setOrdenarPor(String ordenarPor) {
		this.ordenarPor = ordenarPor;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}
}
