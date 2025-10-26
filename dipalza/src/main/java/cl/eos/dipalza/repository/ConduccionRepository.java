package cl.eos.dipalza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.eos.dipalza.entity.Conduccion;


@Repository
public interface ConduccionRepository extends JpaRepository<Conduccion, String>{
	

}
