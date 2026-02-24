package cl.eos.dipalza.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracion")
public class Configuracion {
	// Enum auxiliar
	public enum TipoDato {
		STRING, INTEGER, BOOLEAN, DOUBLE
	}

	@Id
	@Column(length = 50)
	private String propiedad;

	@Column(nullable = false)
	private String valor;

	@Column(name = "tipo", nullable = false)
	@Enumerated(EnumType.STRING)
	private TipoDato tipo; // Usamos un Enum para seguridad

	private String descripcion;

	public String getPropiedad() {
		return propiedad;
	}

	public void setPropiedad(String clave) {
		this.propiedad = clave;
	}

	public String getValor() {
		return valor;
	}

	public void setValor(String valor) {
		this.valor = valor;
	}

	public TipoDato getTipo() {
		return tipo;
	}

	public void setTipo(TipoDato tipo) {
		this.tipo = tipo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	
}
