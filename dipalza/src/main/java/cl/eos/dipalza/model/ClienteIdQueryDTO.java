package cl.eos.dipalza.model;

public class ClienteIdQueryDTO {

	private String rut;
	private String codigo;
	
	

	public ClienteIdQueryDTO(String rut, String codigo) {
		super();
		this.rut = rut;
		this.codigo = codigo;
	}

	public String getRut() {
		return rut;
	}

	public void setRut(String rut) {
		this.rut = rut;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

}
