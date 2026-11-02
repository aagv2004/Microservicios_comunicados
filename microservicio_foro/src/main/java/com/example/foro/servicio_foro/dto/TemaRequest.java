package com.example.foro.servicio_foro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TemaRequest(
        @NotBlank @Size(max = 150) String titulo,
        @NotBlank @Size(max = 10000) String contenido,
        @NotNull @Positive Long categoriaId) {
}
