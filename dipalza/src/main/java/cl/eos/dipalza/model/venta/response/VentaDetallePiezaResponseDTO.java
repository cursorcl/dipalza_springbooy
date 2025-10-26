// file: cl/eos/dipalza/model/venta/VentaDetallePiezaResponseDTO.java
package cl.eos.dipalza.model.venta.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class VentaDetallePiezaResponseDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private BigDecimal peso; // opcional
	private LocalDate creadoEn; // timestamp

	public VentaDetallePiezaResponseDTO() {
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

	public LocalDate getCreadoEn() {
		return creadoEn;
	}

	public void setCreadoEn(LocalDate creadoEn) {
		this.creadoEn = creadoEn;
	}
}
