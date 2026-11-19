package com.example.foro.servicio_moderacion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.foro.servicio_moderacion.security.IdentidadFilter;
import com.example.foro.servicio_moderacion.security.VerificadorIdentidad;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    UserDetailsService sinCuentasLocales() {
        return username -> { throw new UsernameNotFoundException("Moderacion no administra cuentas locales"); };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, VerificadorIdentidad verificador) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/estado").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/moderacion/temas/*", "/api/moderacion/comentarios/*").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> IdentidadFilter.responder(response,
                                401, "Se requiere autenticacion con correo y password"))
                        .accessDeniedHandler((request, response, ex) -> IdentidadFilter.responder(response,
                                403, "No tienes permiso para esta operacion")))
                .addFilterBefore(new IdentidadFilter(verificador), AnonymousAuthenticationFilter.class)
                .build();
    }
}
