package cl.eos.dipalza.model;

import java.math.BigDecimal;

public class IlaDTO {
	private String codigo;
	private String descripcion;
	private BigDecimal valor;

	public IlaDTO() {
	}

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
		this.valor =  valor;
	}
}
