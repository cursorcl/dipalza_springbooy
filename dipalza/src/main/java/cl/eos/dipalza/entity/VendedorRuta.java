package cl.eos.dipalza.entity;

import cl.eos.dipalza.entity.ids.VendedorRutaId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "vendedor_ruta", schema = "dbo")
public class VendedorRuta {

    @EmbeddedId
    private VendedorRutaId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "codigo_vendedor", referencedColumnName = "codigo", insertable = false, updatable = false),
        @JoinColumn(name = "tipo_vendedor", referencedColumnName = "tipo", insertable = false, updatable = false)
    })
    private Vendedor vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_ruta", referencedColumnName = "codigo", insertable = false, updatable = false)
    private Ruta ruta;

    public VendedorRutaId getId() { return id; }
    public void setId(VendedorRutaId id) { this.id = id; }
    public Vendedor getVendedor() { return vendedor; }
    public void setVendedor(Vendedor vendedor) { this.vendedor = vendedor; }
    public Ruta getRuta() { return ruta; }
    public void setRuta(Ruta ruta) { this.ruta = ruta; }
}
