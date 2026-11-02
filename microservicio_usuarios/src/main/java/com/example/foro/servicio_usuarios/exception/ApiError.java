package com.example.foro.servicio_usuarios.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(Instant timestamp, int status, String mensaje, Map<String, String> campos) {
    public static ApiError de(int status, String mensaje) {
        return new ApiError(Instant.now(), status, mensaje, Map.of());
    }
}
