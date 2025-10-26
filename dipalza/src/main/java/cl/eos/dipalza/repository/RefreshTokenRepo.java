package cl.eos.dipalza.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.eos.dipalza.entity.RefreshToken;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenHashAndRevokedFalse(String hash);
}