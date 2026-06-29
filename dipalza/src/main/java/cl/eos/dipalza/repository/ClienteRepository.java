package cl.eos.dipalza.repository;

import cl.eos.dipalza.entity.Cliente;
import cl.eos.dipalza.entity.ids.ClienteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, ClienteId> {
    // Puedes agregar métodos de búsqueda personalizados si los necesitas.
    // Ejemplo: List<Cliente> findByCiudad(String ciudad);
	
	List<Cliente> getClienteByCodigoRuta(String ruta);

	List<Cliente> findByCodigoVendedorOrderByRazonAsc(String codigoVendedor);

}