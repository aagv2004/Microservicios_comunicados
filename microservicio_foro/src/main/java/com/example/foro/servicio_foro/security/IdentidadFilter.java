package com.example.foro.servicio_foro.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.security.core.AuthenticationException;

import com.example.foro.servicio_foro.exception.ApiException;

public class IdentidadFilter extends OncePerRequestFilter {
    private final VerificadorIdentidad verificador;

    public IdentidadFilter(VerificadorIdentidad verificador) {
        this.verificador = verificador;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "GET".equals(request.getMethod()) || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null) {
            chain.doFilter(request, response);
            return;
        }
        try {
            var credenciales = new BasicAuthenticationConverter().convert(request);
            if (credenciales == null) {
                responder(response, 401, "Se requiere autenticacion HTTP Basic");
                return;
            }
            Identidad identidad;
            try {
                identidad = verificador.verificar(credenciales.getName(), credenciales.getCredentials().toString());
            } finally {
                credenciales.eraseCredentials();
            }
            var authentication = new UsernamePasswordAuthenticationToken(identidad, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + identidad.rol().name())));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (ApiException ex) {
            // La respuesta no expone excepciones externas ni credenciales.
            if (ex.getStatus().value() == 503) {
                responder(response, 503, "Servicio de Usuarios no disponible");
            } else {
                responder(response, 401, "Correo o password incorrectos");
            }
            return;
        } catch (AuthenticationException ex) {
            responder(response, 401, "Credenciales HTTP Basic invalidas");
            return;
        }
        chain.doFilter(request, response);
    }

    public static void responder(HttpServletResponse response, int status, String mensaje) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        if (status == 401) response.setHeader("WWW-Authenticate", "Basic realm=\"Foro\"");
        response.getWriter().write("{\"status\":" + status + ",\"mensaje\":\"" + mensaje + "\",\"campos\":{}}");
    }
}
