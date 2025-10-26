package cl.eos.dipalza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.eos.dipalza.entity.Ruta;


@Repository
public interface RutaRepository extends JpaRepository<Ruta, String>{
	

}
