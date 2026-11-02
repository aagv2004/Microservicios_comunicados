package com.example.foro.servicio_foro.dto;

import java.time.Instant;

import com.example.foro.servicio_foro.model.Tema;

public record TemaResponse(Long id, String titulo, String contenido, Long categoriaId,
        String categoriaNombre, Long autorId, Instant creadoEn, Instant actualizadoEn) {
    public static TemaResponse de(Tema tema) {
        return new TemaResponse(tema.getId(), tema.getTitulo(), tema.getContenido(), tema.getCategoria().getId(),
                tema.getCategoria().getNombre(), tema.getAutorId(), tema.getCreadoEn(), tema.getActualizadoEn());
    }
}
