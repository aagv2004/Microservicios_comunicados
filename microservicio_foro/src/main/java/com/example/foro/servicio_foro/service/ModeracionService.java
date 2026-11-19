package com.example.foro.servicio_foro.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.foro.servicio_foro.dto.*;
import com.example.foro.servicio_foro.exception.ApiException;
import com.example.foro.servicio_foro.repository.*;
import com.example.foro.servicio_foro.security.Identidad;

@Service
@Transactional
public class ModeracionService {
    private final TemaRepository temas;
    private final ComentarioRepository comentarios;

    public ModeracionService(TemaRepository temas, ComentarioRepository comentarios) {
        this.temas = temas;
        this.comentarios = comentarios;
    }

    public ModeracionResponse moderarTema(Long id, ModeracionRequest datos, Identidad identidad) {
        exigirAdmin(identidad, id);
        var tema = temas.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tema no encontrado"));
        tema.moderar(datos.bloqueado(), datos.motivo().strip(), identidad.usuarioId());
        temas.saveAndFlush(tema);
        return new ModeracionResponse("TEMA", id, tema.isBloqueado(), tema.getMotivoBloqueo(), tema.getModeradorId());
    }

    public ModeracionResponse moderarComentario(Long id, ModeracionRequest datos, Identidad identidad) {
        exigirAdmin(identidad, id);
        var comentario = comentarios.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comentario no encontrado"));
        comentario.moderar(datos.bloqueado(), datos.motivo().strip(), identidad.usuarioId());
        comentarios.saveAndFlush(comentario);
        return new ModeracionResponse("COMENTARIO", id, comentario.isBloqueado(), comentario.getMotivoBloqueo(), comentario.getModeradorId());
    }

    private void exigirAdmin(Identidad identidad, Long id) {
        if (identidad == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Se requiere autenticacion");
        if (identidad.rol() != Identidad.Rol.ADMIN) throw new AccessDeniedException("Se requiere ADMIN");
        if (id == null || id <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "El ID debe ser positivo");
    }
}
