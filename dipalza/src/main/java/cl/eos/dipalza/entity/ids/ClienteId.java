package cl.eos.dipalza.entity.ids;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ClienteId implements Serializable {
    private static final long serialVersionUID = 1L;

	@Column(name = "rut", length = 10, nullable = false)
    private String rut;

    @Column(name = "codigo", length = 3, nullable = false)
    private String codigo;

    public ClienteId() { }

    public ClienteId(String rut, String codigo) {
        this.rut = rut;
        this.codigo = codigo;
    }

    public String getRut() { return rut; }
    public void setRut(String rut) { this.rut = rut; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClienteId)) return false;
        ClienteId that = (ClienteId) o;
        return Objects.equals(rut, that.rut) && Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rut, codigo);
    }
}