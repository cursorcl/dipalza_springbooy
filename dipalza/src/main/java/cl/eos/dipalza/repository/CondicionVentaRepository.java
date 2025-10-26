package cl.eos.dipalza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.eos.dipalza.entity.CondicionVenta;


@Repository
public interface CondicionVentaRepository extends JpaRepository<CondicionVenta, String>{
	

}
