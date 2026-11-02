package com.example.foro.servicio_foro.security;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.example.foro.servicio_foro.exception.ApiException;

@Component
public class UsuariosClient implements VerificadorIdentidad {
    private final RestClient client;

    public UsuariosClient(@Value("${usuarios.base-url}") String baseUrl) {
        var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public Identidad verificar(String correo, String password) {
        try {
            var usuario = client.get().uri("/api/auth/me")
                    .headers(headers -> headers.setBasicAuth(correo, password, StandardCharsets.UTF_8))
                    .retrieve().body(UsuarioResponse.class);
            if (usuario == null || usuario.id() == null || usuario.id() <= 0 || usuario.rol() == null) {
                throw noDisponible();
            }
            return new Identidad(usuario.id(), usuario.rol());
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Correo o password incorrectos");
            }
            throw noDisponible();
        } catch (RestClientException | IllegalArgumentException ex) {
            throw noDisponible();
        }
    }

    private ApiException noDisponible() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de Usuarios no disponible");
    }

    public record UsuarioResponse(Long id, String nombre, String correo, Identidad.Rol rol) {
    }
}
