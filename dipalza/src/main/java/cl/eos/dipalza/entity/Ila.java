package cl.eos.dipalza.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ila") // El nombre de tu tabla
public class Ila {
	@Id
	@Column(name = "codigo", length = 10, nullable = false)
	private String codigo;
	@Column(name = "descripcion", length = 50, nullable = false)
	private String descripcion;

	@Column(name = "valor", columnDefinition = "money")
	private BigDecimal valor;

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

}
