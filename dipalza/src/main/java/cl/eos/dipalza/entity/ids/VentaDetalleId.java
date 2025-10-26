package cl.eos.dipalza.entity.ids;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class VentaDetalleId implements Serializable {
    private static final long serialVersionUID = 1L;

	@Column(name = "venta_id", length = 36)
    private Long ventaId;

    @Column(name = "linea")
    private Integer linea;

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

	@Override
	public int hashCode() {
		return Objects.hash(linea, ventaId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VentaDetalleId other = (VentaDetalleId) obj;
		return Objects.equals(linea, other.linea) && Objects.equals(ventaId, other.ventaId);
	}

    
    
}
