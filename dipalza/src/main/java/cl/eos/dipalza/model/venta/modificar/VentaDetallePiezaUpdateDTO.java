package cl.eos.dipalza.model.venta.modificar;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VentaDetallePiezaUpdateDTO {

	private Long id;
	private Long ventaId;
	private Integer linea;
	private BigDecimal peso;
	private LocalDate creadoEn;
	private Long invId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getVentaId() {
		return ventaId;
	}

	public void setVentaId(Long ventaId) {
		this.ventaId = ventaId;
	}

	public Integer getLinea() {
		return linea;
	}

	public void setLinea(Integer linea) {
		this.linea = linea;
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

	public Long getInvId() {
		return invId;
	}

	public void setInvId(Long invId) {
		this.invId = invId;
	}
	
	
	
}
