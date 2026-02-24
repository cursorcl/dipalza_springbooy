package cl.eos.dipalza.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
// ... imports existentes ...
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

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
    // Agregamos inyección para refresh (asegúrate de que esta property exista en application.properties)
    @Value("${security.jwt.refresh-hr}") private long refreshHr; 

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 1. Método existente (Access Token)
    public String generateAccess(AppUser u) {
        var roles = u.getRoles().stream().map(AppRole::getName).collect(Collectors.toList());
        return buildToken(u.getUsername(), roles, accessMin, ChronoUnit.MINUTES, "ACCESS");
    }

    // 2. NUEVO: Método para Refresh Token (JWT)
    public String generateRefresh(AppUser u) {
        // No solemos meter roles en el refresh token para hacerlo más ligero, 
        // pero sí un identificador único (jti) si quisieras.
        return buildToken(u.getUsername(), null, refreshHr, ChronoUnit.HOURS, "REFRESH");
    }

    // Método auxiliar para no repetir código
    private String buildToken(String subject, Object roles, long duration, ChronoUnit unit, String type) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(duration, unit)))
                .claim("type", type); // Etiqueta para diferenciar access de refresh
        
        if (roles != null) {
            builder.claim("roles", roles);
        }
        
        return builder.signWith(key(), Jwts.SIG.HS256).compact();
    }

    // 3. Método para extraer el usuario (subject) del token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 4. Método genérico para extraer claims
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parse(token).getPayload();
        return claimsResolver.apply(claims);
    }

    // Tu método parse existente (actualizado a JJWT moderno)
    public Jws<Claims> parse(String token) {
        return Jwts.parser()
            .verifyWith(key())
            .build()
            .parseSignedClaims(token);
    }
}