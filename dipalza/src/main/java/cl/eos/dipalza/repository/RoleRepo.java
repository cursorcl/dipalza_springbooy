package cl.eos.dipalza.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.eos.dipalza.entity.AppRole;

public interface RoleRepo extends JpaRepository<AppRole, Long> {
	  Optional<AppRole> findByName(String name);
	}