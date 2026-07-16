package cl.eos.dipalza.repository;

import cl.eos.dipalza.entity.VendedorRuta;
import cl.eos.dipalza.entity.ids.VendedorRutaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendedorRutaRepository extends JpaRepository<VendedorRuta, VendedorRutaId> {
    List<VendedorRuta> findByIdCodigoVendedorAndIdTipoVendedor(String codigoVendedor, String tipoVendedor);
    void deleteByIdCodigoVendedorAndIdTipoVendedor(String codigoVendedor, String tipoVendedor);
}
