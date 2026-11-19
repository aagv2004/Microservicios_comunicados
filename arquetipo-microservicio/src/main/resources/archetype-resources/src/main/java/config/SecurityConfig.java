package ${package}.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Conectar la identidad de Usuarios antes de habilitar endpoints del negocio.
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/estado").permitAll()
                .anyRequest().denyAll()).build();
    }
}
