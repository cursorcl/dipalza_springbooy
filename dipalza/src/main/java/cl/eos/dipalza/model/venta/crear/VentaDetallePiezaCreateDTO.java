// file: cl/eos/dipalza/model/venta/VentaDetallePiezaCreateDTO.java
package cl.eos.dipalza.model.venta.crear;

import java.io.Serializable;
import java.math.BigDecimal;

public class VentaDetallePiezaCreateDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id; // obligatorio (único global)
	private BigDecimal peso; // opcional

	public VentaDetallePiezaCreateDTO() {
	}

	public VentaDetallePiezaCreateDTO(Long id, BigDecimal peso) {
		this.id = id;
		this.peso = peso;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public BigDecimal getPeso() {
		return peso;
	}

	public void setPeso(BigDecimal peso) {
		this.peso = peso;
	}
}
