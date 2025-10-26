package cl.eos.dipalza.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.eos.dipalza.entity.Vendedor;
import cl.eos.dipalza.entity.ids.VendedorId;

@Repository
public interface VendedorRepository extends JpaRepository<Vendedor, VendedorId> {

	Optional<Vendedor> findFirstByRutOrderByNombreAsc(String rut);
}
