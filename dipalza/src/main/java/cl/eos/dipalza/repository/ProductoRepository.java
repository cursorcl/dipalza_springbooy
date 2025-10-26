package cl.eos.dipalza.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.eos.dipalza.entity.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, String> {

	List<Producto> getProductosByDescripcion(String descripcion);
	
}