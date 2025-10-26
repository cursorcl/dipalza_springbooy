package cl.eos.dipalza.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.eos.dipalza.entity.Cliente;
import cl.eos.dipalza.entity.ids.ClienteId;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, ClienteId> {
    // Puedes agregar métodos de búsqueda personalizados si los necesitas.
    // Ejemplo: List<Cliente> findByCiudad(String ciudad);
	
	List<Cliente> getClienteByCodigoRuta(String ruta);
}