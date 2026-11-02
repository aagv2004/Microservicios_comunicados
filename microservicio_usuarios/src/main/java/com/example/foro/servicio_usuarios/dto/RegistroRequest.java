package com.example.foro.servicio_usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank @Size(min = 10, max = 64) String password) {
    @Override
    public String toString() {
        return "RegistroRequest[datos protegidos]";
    }
}
