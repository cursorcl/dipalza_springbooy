package cl.eos.dipalza.model.venta.crear;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public class VentaCreateDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private LocalDate fecha;
	private String rutCliente;
	private String codigoCliente; 
	private String codigoVendedor; 
	private String tipoVendedor; 
	private String codigoRuta;
	private List<VentaDetalleCreateDTO> detalles;

	public VentaCreateDTO() {
	}

	public LocalDate getFecha() {
		return fecha;
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

	public void setFecha(LocalDate fecha) {
		this.fecha = fecha;
	}

	public String getCodigoRuta() {
		return codigoRuta;
	}
	public void setCodigoRuta(String codigoRuta) {
		this.codigoRuta = codigoRuta;
	}

	public List<VentaDetalleCreateDTO> getDetalles() {
		return detalles;
	}

	public void setDetalles(List<VentaDetalleCreateDTO> detalles) {
		this.detalles = detalles;
	}
}
