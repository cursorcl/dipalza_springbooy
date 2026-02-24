package cl.eos.dipalza.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ruta") // El nombre de tu tabla
public class Ruta {
	@Id
	@Column(name = "codigo", length = 10, nullable = false)
	private String codigo;
	@Column(name = "descripcion", length = 50, nullable = false)
	private String descripcion;

	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conduccion_id", nullable = false)
    private Conduccion conduccion;
	
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

	public Conduccion getConduccion() {
		return conduccion;
	}

	public void setConduccion(Conduccion conduccion) {
		this.conduccion = conduccion;
	}
	
	
}
