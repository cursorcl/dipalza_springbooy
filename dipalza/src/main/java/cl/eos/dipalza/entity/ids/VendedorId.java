package cl.eos.dipalza.entity.ids;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class VendedorId implements Serializable {
    private static final long serialVersionUID = 1L;
	private String codigo; // varchar(3)
    private String tipo;   // varchar(1)

    public VendedorId() {}
    public VendedorId(String codigo, String tipo) {
        this.codigo = codigo; this.tipo = tipo;
    }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VendedorId that)) return false;
        return Objects.equals(codigo, that.codigo) && Objects.equals(tipo, that.tipo);
    }
    @Override public int hashCode() { return Objects.hash(codigo, tipo); }
}
