package com.example.foro.servicio_foro.dto;

import com.example.foro.servicio_foro.model.Categoria;

public record CategoriaResponse(Long id, String nombre, String descripcion) {
    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre(), categoria.getDescripcion());
    }
}
