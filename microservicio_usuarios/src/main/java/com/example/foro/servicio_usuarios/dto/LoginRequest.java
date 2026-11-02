package com.example.foro.servicio_usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank @Size(max = 64) String password) {
    @Override
    public String toString() {
        return "LoginRequest[datos protegidos]";
    }
}
