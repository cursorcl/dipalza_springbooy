package cl.eos.dipalza.entity.ids;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class VendedorRutaId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "codigo_vendedor", length = 3, nullable = false)
    private String codigoVendedor;
    @Column(name = "tipo_vendedor", length = 1, nullable = false)
    private String tipoVendedor;
    @Column(name = "codigo_ruta", length = 10, nullable = false)
    private String codigoRuta;

    public VendedorRutaId() {}

    public VendedorRutaId(String codigoVendedor, String tipoVendedor, String codigoRuta) {
        this.codigoVendedor = codigoVendedor;
        this.tipoVendedor = tipoVendedor;
        this.codigoRuta = codigoRuta;
    }

    public String getCodigoVendedor() { return codigoVendedor; }
    public void setCodigoVendedor(String codigoVendedor) { this.codigoVendedor = codigoVendedor; }
    public String getTipoVendedor() { return tipoVendedor; }
    public void setTipoVendedor(String tipoVendedor) { this.tipoVendedor = tipoVendedor; }
    public String getCodigoRuta() { return codigoRuta; }
    public void setCodigoRuta(String codigoRuta) { this.codigoRuta = codigoRuta; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VendedorRutaId that)) return false;
        return Objects.equals(codigoVendedor, that.codigoVendedor)
                && Objects.equals(tipoVendedor, that.tipoVendedor)
                && Objects.equals(codigoRuta, that.codigoRuta);
    }

    @Override
    public int hashCode() { return Objects.hash(codigoVendedor, tipoVendedor, codigoRuta); }
}
