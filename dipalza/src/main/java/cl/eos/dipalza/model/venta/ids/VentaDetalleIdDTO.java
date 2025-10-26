// file: cl/eos/dipalza/model/venta/VentaDetalleIdDTO.java
package cl.eos.dipalza.model.venta.ids;

import java.io.Serializable;
import java.util.Objects;

public class VentaDetalleIdDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long ventaId;
	private Integer linea;

	public VentaDetalleIdDTO() {
	}

	public VentaDetalleIdDTO(Long ventaId, Integer linea) {
		this.ventaId = ventaId;
		this.linea = linea;
	}

	public Long getVentaId() {
		return ventaId;
	}

	public void setVentaId(Long ventaId) {
		this.ventaId = ventaId;
	} // typo fix below

	public Integer getLinea() {
		return linea;
	}

	public void setLinea(Integer linea) {
		this.linea = linea;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof VentaDetalleIdDTO))
			return false;
		VentaDetalleIdDTO that = (VentaDetalleIdDTO) o;
		return Objects.equals(ventaId, that.ventaId) && Objects.equals(linea, that.linea);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ventaId, linea);
	}
}
