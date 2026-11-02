package com.example.foro.servicio_usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 150) String correo) {
}
