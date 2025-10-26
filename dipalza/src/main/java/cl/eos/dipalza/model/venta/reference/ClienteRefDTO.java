// file: cl/eos/dipalza/model/venta/VendedorRefDTO.java
package cl.eos.dipalza.model.venta.reference;

import java.io.Serializable;

public class ClienteRefDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private String rut; // obligatorio
	private String codigo; // obligatorio
	private String nombre;
	

	public ClienteRefDTO() {
	}

	public ClienteRefDTO(String rut, String codigo, String nombre) {
		this.codigo = codigo;
		this.rut = rut;
		this.nombre = nombre;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getRut() {
		return rut;
	}

	public void setRut(String rut) {
		this.rut = rut;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	
}
