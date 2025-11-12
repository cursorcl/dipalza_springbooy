// file: cl/eos/dipalza/model/venta/VentaDetallePiezaCreateDTO.java
package cl.eos.dipalza.model.venta;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class VentaDetallePiezaDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id; // obligatorio (único global)
	private BigDecimal peso; // opcional
	private Long invId;
	private LocalDate creadoEn;
	

	public VentaDetallePiezaDTO() {
	}

	public VentaDetallePiezaDTO(Long id, BigDecimal peso) {
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

	public Long getInvId() {
		return invId;
	}

	public void setInvId(Long invId) {
		this.invId = invId;
	}

	public LocalDate getCreadoEn() {
		return creadoEn;
	}

	public void setCreadoEn(LocalDate creadoEn) {
		this.creadoEn = creadoEn;
	}
	
	
}
