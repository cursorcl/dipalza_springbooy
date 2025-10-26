package cl.eos.dipalza.entity;

import cl.eos.dipalza.entity.ids.ClienteId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente") // El nombre de tu tabla
public class Cliente {

	@EmbeddedId
	private ClienteId id;

	@Column(name = "razon", length = 60)
	private String razon;

	@Column(length = 40)
	private String direccion;

	@Column(length = 30)
	private String ciudad;

	@Column(length = 40)
	private String giro;

	@Column(length = 40)
	private String telefono;

	@Column(name = "codigo_ruta", length = 10)
	private String codigoRuta;

	@Column(name = "codigo_vendedor", length = 3)
	private String codigoVendedor;

	public ClienteId getId() {
		return id;
	}

	public void setId(ClienteId id) {
		this.id = id;
	}

	public String getRazon() {
		return razon;
	}

	public void setRazon(String razon) {
		this.razon = razon;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	public String getCiudad() {
		return ciudad;
	}

	public void setCiudad(String ciudad) {
		this.ciudad = ciudad;
	}

	public String getGiro() {
		return giro;
	}

	public void setGiro(String giro) {
		this.giro = giro;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public String getCodigoRuta() {
		return codigoRuta;
	}

	public void setCodigoRuta(String codigoRuta) {
		this.codigoRuta = codigoRuta;
	}

	public String getCodigoVendedor() {
		return codigoVendedor;
	}

	public void setCodigoVendedor(String codigoVendedor) {
		this.codigoVendedor = codigoVendedor;
	}
}
