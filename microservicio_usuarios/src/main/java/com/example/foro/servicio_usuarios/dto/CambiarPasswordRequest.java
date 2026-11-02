package com.example.foro.servicio_usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
        @NotBlank @Size(max = 64) String passwordActual,
        @NotBlank @Size(min = 10, max = 64) String passwordNueva) {
    @Override
    public String toString() {
        return "CambiarPasswordRequest[datos protegidos]";
    }
}
