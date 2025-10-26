package cl.eos.dipalza.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import cl.eos.dipalza.repository.UserRepo;
import cl.eos.dipalza.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthFilter extends OncePerRequestFilter {
	  @Autowired JwtService jwt;
	  @Autowired UserRepo users;

	  @Override protected void doFilterInternal(@NonNull   HttpServletRequest req, @NonNull   HttpServletResponse res, @NonNull   FilterChain fc)
	      throws ServletException, IOException {
	    String h = req.getHeader("Authorization");
	    if (h != null && h.startsWith("Bearer ")) {
	      try {
	        var jws = jwt.parse(h.substring(7));
	        String username = jws.getPayload().getSubject();
	        var user = users.findByUsername(username).orElseThrow();
	        var authorities = user.getRoles().stream()
	           .map(r -> new SimpleGrantedAuthority(r.getName()))
	           .toList();
	        var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
	        SecurityContextHolder.getContext().setAuthentication(auth);
	      } catch (RuntimeException ignored) {
	        // token inválido -> sin autenticación
	      }
	    }
	    fc.doFilter(req, res);
	  }
	}

