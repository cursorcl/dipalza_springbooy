package cl.eos.dipalza.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import cl.eos.dipalza.entity.AppUser;
import cl.eos.dipalza.entity.RefreshToken;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
	
	
	Optional<RefreshToken> findByTokenHashAndRevokedFalse(String hash);

	Optional<RefreshToken> findByTokenHash(String token);

	void deleteByExpiresAtBefore(Instant now);
	
	@Modifying
	int deleteByUser(AppUser user);
}