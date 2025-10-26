package cl.eos.dipalza.entity.ids;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DetalleDocumentoId implements Serializable {

    private static final long serialVersionUID = 1L;

	@Column(name = "Id", length = 10)
    private String id;

    @Column(name = "Linea", length = 3)
    private String linea;

    // Constructor vacío requerido por JPA
    public DetalleDocumentoId() {}

    // Constructor para conveniencia
    public DetalleDocumentoId(String id, String linea) {
        this.id = id;
        this.linea = linea;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLinea() { return linea; }
    public void setLinea(String linea) { this.linea = linea; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetalleDocumentoId that = (DetalleDocumentoId) o;
        return Objects.equals(id, that.id) && Objects.equals(linea, that.linea);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, linea);
    }
}