package com.example.foro.servicio_moderacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModeracionRequest(@NotNull Boolean bloqueado,
        @NotBlank @Size(max = 500) String motivo) {}
