package cl.eos.dipalza.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey; // <-- IMPORTANTE

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import cl.eos.dipalza.entity.AppRole;
import cl.eos.dipalza.entity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
@Profile({"dev-sec","prod-sec"})
public class JwtService {

  @Value("${security.jwt.secret}") private String secret;
  @Value("${security.jwt.issuer}") private String issuer;
  @Value("${security.jwt.access-minutes}") private long accessMin;

  private SecretKey key() {
    // Requiere secreto >= 32 bytes para HS256
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccess(AppUser u) {
    var roles = u.getRoles().stream().map(AppRole::getName).collect(Collectors.toList());
    Instant now = Instant.now();
    return Jwts.builder()
        .issuer(issuer)
        .subject(u.getUsername())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(accessMin, ChronoUnit.MINUTES)))
        .claim("roles", roles)
        .signWith(key(), Jwts.SIG.HS256) // <-- especifica algoritmo
        .compact();
  }

  public Jws<Claims> parse(String token) {
    return Jwts.parser()
        .verifyWith(key())   // <-- ahora es SecretKey
        .build()
        .parseSignedClaims(token);
  }
}
