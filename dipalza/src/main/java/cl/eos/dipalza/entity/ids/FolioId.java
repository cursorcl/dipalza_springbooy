package cl.eos.dipalza.entity.ids;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;

public class FolioId implements Serializable {
	private static final long serialVersionUID = 1L;

	@Column(name = "numero", length = 7)
	private String numero;

	@Column(name = "tipo", length = 2)
	private String tipo;

	@Column(name = "tipo1", length = 1)
	private String tipo1;

	// JPA requiere un constructor sin argumentos
	public FolioId() {
	}

	// Constructor con argumentos para conveniencia
	public FolioId(String numero, String tipo, String tipo1) {
		this.numero = numero;
		this.tipo = tipo;
		this.tipo1 = tipo1;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getTipo1() {
		return tipo1;
	}

	public void setTipo1(String tipo1) {
		this.tipo1 = tipo1;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		FolioId folioId = (FolioId) o;
		return Objects.equals(numero, folioId.numero) && Objects.equals(tipo, folioId.tipo)
				&& Objects.equals(tipo1, folioId.tipo1);
	}

	@Override
	public int hashCode() {
		return Objects.hash(numero, tipo, tipo1);
	}
}
