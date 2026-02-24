package cl.eos.dipalza.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import cl.eos.dipalza.entity.AppUser;
import cl.eos.dipalza.entity.RefreshToken;
import cl.eos.dipalza.entity.Vendedor;
import cl.eos.dipalza.mapper.VendedorMapper;
import cl.eos.dipalza.model.VendedorDTO;
import cl.eos.dipalza.repository.RefreshTokenRepo;
import cl.eos.dipalza.repository.UserRepo;
import cl.eos.dipalza.repository.VendedorRepository;
import cl.eos.dipalza.service.JwtService;

@RestController
@RequestMapping("/auth")
@Profile({"dev-sec","prod-sec"})
public class AuthController {

	private final UserRepo users;
	private final PasswordEncoder enc;
	private final JwtService jwt;
	private final RefreshTokenRepo refreshTokenRepo;
	private final VendedorRepository vendedorRepo;
	@Value("${security.jwt.refresh-hr}")
	long refreshHr;

	public record LoginReq(String username, String password) {
	}

	public record TokenResponse(String accessToken, String refreshToken, long expiresInSeconds, VendedorDTO vendedor) {
	}
	

	public record WebLoginRes(String token, String refreshToken, long expiresInSeconds, long id, String username, String firstName, String lastName ) {
	}

	public AuthController(UserRepo users, VendedorRepository vendedorRepo, PasswordEncoder enc, JwtService jwt, RefreshTokenRepo rtRepo) {
		this.users = users;
		this.enc = enc;
		this.jwt = jwt;
		this.refreshTokenRepo = rtRepo;
		this.vendedorRepo = vendedorRepo;
	}

	@PostMapping("/login")
	public TokenResponse login(@RequestBody LoginReq req) {
		var u = users.findByUsername(req.username())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		
		
		if (!u.isEnabled() || u.isLocked() || !enc.matches(req.password(), u.getPassword()))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

		// buscar Vendedor
	    var vendedorOpt = vendedorRepo.findFirstByRutOrderByNombreAsc(u.getUsername());
	    VendedorDTO vendedorDto = vendedorOpt.map(VendedorMapper::toDto).orElse(null);
		
		
		return generateTokenRes(u, vendedorDto);
	}
	
	@PostMapping("/refresh")
	public TokenResponse refresh(@RequestBody RefreshReq req) {
		
		String hashToken = hashToken(req.refreshToken);
		Optional<RefreshToken> token = refreshTokenRepo.findByTokenHash(hashToken);
		
		if(token.isEmpty())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		
		
			
		RefreshToken t = token.get();
	    if (t.isRevoked() || !t.getExpiresAt().isAfter(Instant.now())) {
	    	throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
	    }
		t.setRevoked(true);
		refreshTokenRepo.save(t);
		var u = t.getUser();
		
	    var vendedorOpt = vendedorRepo.findFirstByRutOrderByNombreAsc(u.getUsername());
	    VendedorDTO vendedorDto = vendedorOpt.map(VendedorMapper::toDto).orElse(null);
	    
	    return generateTokenRes(u, vendedorDto);
	}
	
	
	private TokenResponse generateTokenRes(AppUser u, VendedorDTO vendedorDTO)
	{
		
		String access = jwt.generateAccess(u);
		String refreshJwt = jwt.generateRefresh(u);
        // Hashear con SHA-256 (NO BCrypt) para guardar en BD
        String refreshHash = hashToken(refreshJwt);
        
		var rt = new RefreshToken();
		rt.setUser(u);
		rt.setTokenHash(refreshHash);
		rt.setExpiresAt(Instant.now().plus(refreshHr, ChronoUnit.HOURS));
		refreshTokenRepo.save(rt);

		return new TokenResponse(access, refreshJwt, refreshHr * 60L * 60L, vendedorDTO);
	}
	
	
	@PostMapping("/weblogin")
	public WebLoginRes weblogin(@RequestBody LoginReq req) {
		var u = users.findByUsername(req.username())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		
		
		if (!u.isEnabled() || u.isLocked() || !enc.matches(req.password(), u.getPassword()))
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

		// buscar Vendedor
	    var vendedorOpt = vendedorRepo.findFirstByRutOrderByNombreAsc(u.getUsername());
	    if(vendedorOpt.isEmpty())
	    	throw new ResponseStatusException(HttpStatus.NOT_FOUND);
	    
	    
	    Vendedor vendedor = vendedorOpt.get();
		String access = jwt.generateAccess(u);

		// refresh aleatorio + hash en BD
		String refreshRaw = UUID.randomUUID().toString() + UUID.randomUUID();
		String refreshHash = new BCryptPasswordEncoder().encode(refreshRaw);
		var rt = new RefreshToken();
		rt.setUser(u);
		rt.setTokenHash(refreshHash);
		rt.setExpiresAt(Instant.now().plus(refreshHr, ChronoUnit.MILLIS));
		refreshTokenRepo.save(rt);
		Long id = u.getId();
		String userName = u.getUsername();
		String[] names = vendedor.getNombre().split(" ");
		String firstName = names[0];
		String lastName = names.length > 1 ? names[1] : "";
		
		

		return new WebLoginRes(access, refreshRaw, 60L * 10, id, userName, firstName, lastName); // 10 min si así configuraste
	}

	public record RefreshReq(String refreshToken) {
	}


	
	@PostMapping("/webrefresh")
	public WebLoginRes webRefresh(@RequestBody RefreshReq req) {
		// Busca por hash (compara con BCrypt)
		var tokens = refreshTokenRepo.findAll(); // optimiza con consulta; aquí ejemplo simple
		var match = tokens.stream().filter(t -> !t.isRevoked() && t.getExpiresAt().isAfter(Instant.now()))
				.filter(t -> new BCryptPasswordEncoder().matches(req.refreshToken(), t.getTokenHash())).findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

		
		
		// Rotación: revoca el actual y emite nuevo
		match.setRevoked(true);
		refreshTokenRepo.save(match);
		var u = match.getUser();
		
	    // buscar Vendedor
	    var vendedorOpt = vendedorRepo.findFirstByRutOrderByNombreAsc(u.getUsername());
	    if(vendedorOpt.isEmpty())
	    	throw new ResponseStatusException(HttpStatus.NOT_FOUND);
	    
	    Vendedor vendedor = vendedorOpt.get();
	    
		String access = jwt.generateAccess(u);
		String newRefreshRaw = UUID.randomUUID().toString() + ":" + UUID.randomUUID();
		String newRefreshHash = new BCryptPasswordEncoder().encode(newRefreshRaw);
		var rt = new RefreshToken();
		rt.setUser(u);
		rt.setTokenHash(newRefreshHash);
		rt.setExpiresAt(Instant.now().plus(refreshHr, ChronoUnit.HOURS));
		refreshTokenRepo.save(rt);
		Long id = u.getId();
		String userName = u.getUsername();
		String[] names = vendedor.getNombre().split(" ");
		String firstName = names[0];
		String lastName = names.length > 1 ? names[1] : "";
		
		

		return new WebLoginRes(access, newRefreshRaw, 60L * 10, id, userName, firstName, lastName); // 10 min si así configuraste
	}
	
	
	private String hashToken(String token) {
	    try {
	        MessageDigest digest = MessageDigest.getInstance("SHA-256");
	        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
	        return Base64.getEncoder().encodeToString(hash);
	    } catch (Exception e) {
	        throw new RuntimeException("Error hashing token", e);
	    }
	}
}
