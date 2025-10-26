package cl.eos.dipalza.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.eos.dipalza.entity.Ila;


@Repository
public interface IlaRepository extends JpaRepository<Ila, String>{
	
    // ASC
    List<Ila> findAllByOrderByDescripcionAsc();

    // DESC
    List<Ila> findAllByOrderByDescripcionDesc();

    // Con paginación
    Page<Ila> findAllByOrderByDescripcionAsc(Pageable pageable);
}
