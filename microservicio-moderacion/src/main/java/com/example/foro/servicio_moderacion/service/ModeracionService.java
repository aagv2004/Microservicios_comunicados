package com.example.foro.servicio_moderacion.service;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import com.example.foro.servicio_moderacion.dto.*;
import com.example.foro.servicio_moderacion.exception.ApiException;

@Service
public class ModeracionService {
    private final RestClient foro;

    public ModeracionService(@Value("${foro.base-url}") String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        foro = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public ModeracionResponse moderar(String tipo, Long id, ModeracionRequest datos, String authorization) {
        if (id == null || id <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "El ID debe ser positivo");
        try {
            // Foro verifica nuevamente las credenciales y conserva la propiedad de sus datos.
            var resultado = foro.put().uri("/api/moderacion/{tipo}/{id}", tipo, id)
                    .header("Authorization", authorization).body(datos)
                    .retrieve().body(ModeracionResponse.class);
            if (resultado == null) throw noDisponible();
            return resultado;
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 400 || status == 401 || status == 403 || status == 404 || status == 409) {
                throw new ApiException(HttpStatus.valueOf(status), "Foro rechazo la operacion de moderacion");
            }
            throw noDisponible();
        } catch (RestClientException ex) {
            throw noDisponible();
        }
    }

    private ApiException noDisponible() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de Foro no disponible");
    }
}
