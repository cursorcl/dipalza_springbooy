package cl.eos.dipalza.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @SuppressWarnings("null")
	@Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")               // 1. Aplica a TODAS las rutas de la API
                .allowedOrigins("http://localhost:4200") // 2. Solo permite a tu Angular
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 3. Verbos permitidos
                .allowedHeaders("*")             // 4. Permite todos los headers (necesario para JWT)
                .allowCredentials(true);         // 5. Permite cookies o credenciales si fuera necesario
    }
}
