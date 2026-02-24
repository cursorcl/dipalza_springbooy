package cl.eos.dipalza.model;

public class RutaDTO {
	private String codigo;
	private String descripcion;
	private String codigoConduccion;
	private String nombreConduccion;

	public RutaDTO() {
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

	public String getCodigoConduccion() {
		return codigoConduccion;
	}

	public void setCodigoConduccion(String codigoConduccion) {
		this.codigoConduccion = codigoConduccion;
	}

	public String getNombreConduccion() {
		return nombreConduccion;
	}

	public void setNombreConduccion(String nombreConduccion) {
		this.nombreConduccion = nombreConduccion;
	}
	
	
}
