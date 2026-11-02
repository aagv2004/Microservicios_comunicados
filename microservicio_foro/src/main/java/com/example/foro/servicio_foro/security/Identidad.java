package com.example.foro.servicio_foro.security;

public record Identidad(Long usuarioId, Rol rol) {
    public Identidad {
        if (usuarioId == null || usuarioId <= 0 || rol == null) {
            throw new IllegalArgumentException("La identidad debe tener ID positivo y rol conocido");
        }
    }

    public enum Rol {
        USUARIO, ADMIN
    }
}
