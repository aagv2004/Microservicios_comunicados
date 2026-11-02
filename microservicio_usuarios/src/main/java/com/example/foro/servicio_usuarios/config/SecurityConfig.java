package com.example.foro.servicio_usuarios.config;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.example.foro.servicio_usuarios.repository.UsuarioRepository;
import com.example.foro.servicio_usuarios.service.UsuarioService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(UsuarioRepository repository) {
        return correo -> {
            var usuario = repository.findByCorreo(UsuarioService.normalizarCorreo(correo))
                    .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
            // El principal conserva el ID para comprobar la propiedad de la cuenta.
            return User.withUsername(usuario.getId().toString())
                    .password(usuario.getPasswordHash()).roles(usuario.getRol().name()).build();
        };
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager manager) throws Exception {
        return http
                // La API recibe credenciales por peticion y no mantiene una sesion por cookies.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationManager(manager)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("ADMIN")
                        .requestMatchers("/api/usuarios/**", "/api/auth/me").authenticated()
                        .anyRequest().denyAll())
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, ex) ->
                        responder(response, 401, "Correo o password incorrectos")))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> responder(response, 401,
                                "Se requiere autenticacion con correo y password"))
                        .accessDeniedHandler((request, response, ex) -> responder(response, 403,
                                "No tienes permiso para esta operacion")))
                .build();
    }

    private static void responder(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        if (status == 401) response.setHeader("WWW-Authenticate", "Basic realm=\"Usuarios\"");
        response.getWriter().write("{\"status\":" + status + ",\"mensaje\":\"" + mensaje + "\",\"campos\":{}}");
    }
}
