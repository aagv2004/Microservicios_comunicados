package com.example.foro.servicio_foro.dto;

import java.time.Instant;

import com.example.foro.servicio_foro.model.Comentario;

public record ComentarioResponse(Long id, String contenido, Long temaId, Long autorId,
        Instant creadoEn, Instant actualizadoEn) {
    public static ComentarioResponse de(Comentario comentario) {
        return new ComentarioResponse(comentario.getId(), comentario.getContenido(), comentario.getTema().getId(),
                comentario.getAutorId(), comentario.getCreadoEn(), comentario.getActualizadoEn());
    }
}
