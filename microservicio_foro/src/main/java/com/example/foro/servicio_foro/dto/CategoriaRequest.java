package com.example.foro.servicio_foro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank @Size(max = 60) String nombre,
        @NotBlank @Size(max = 250) String descripcion) {
}
