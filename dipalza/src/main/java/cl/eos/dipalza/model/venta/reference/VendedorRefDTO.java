// file: cl/eos/dipalza/model/venta/VendedorRefDTO.java
package cl.eos.dipalza.model.venta.reference;

import java.io.Serializable;

public class VendedorRefDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private String codigo; // obligatorio
	private String tipo; // opcional

	public VendedorRefDTO() {
	}

	public VendedorRefDTO(String codigo, String tipo) {
		this.codigo = codigo;
		this.tipo = tipo;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}
}
