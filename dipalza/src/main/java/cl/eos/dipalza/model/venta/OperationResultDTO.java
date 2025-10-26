package cl.eos.dipalza.model.venta;

import java.io.Serializable;
import java.util.Map;

public class OperationResultDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private boolean ok;
	private String mensaje;
	private Map<String, Object> detalles; // opcional: errores por campo/línea

	public OperationResultDTO() {
	}

	public OperationResultDTO(boolean ok, String mensaje, Map<String, Object> detalles) {
		this.ok = ok;
		this.mensaje = mensaje;
		this.detalles = detalles;
	}

	public boolean isOk() {
		return ok;
	}

	public void setOk(boolean ok) {
		this.ok = ok;
	}

	public String getMensaje() {
		return mensaje;
	}

	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}

	public Map<String, Object> getDetalles() {
		return detalles;
	}

	public void setDetalles(Map<String, Object> detalles) {
		this.detalles = detalles;
	}
}
