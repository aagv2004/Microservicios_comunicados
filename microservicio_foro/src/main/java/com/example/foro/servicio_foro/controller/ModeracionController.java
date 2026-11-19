package com.example.foro.servicio_foro.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.foro.servicio_foro.dto.*;
import com.example.foro.servicio_foro.security.Identidad;
import com.example.foro.servicio_foro.service.ModeracionService;

@RestController
@RequestMapping("/api/moderacion")
public class ModeracionController {
    private final ModeracionService moderacion;
    public ModeracionController(ModeracionService moderacion) { this.moderacion = moderacion; }

    @PutMapping("/temas/{id}")
    public ModeracionResponse tema(@PathVariable Long id, @Valid @RequestBody ModeracionRequest datos,
            @AuthenticationPrincipal Identidad identidad) {
        return moderacion.moderarTema(id, datos, identidad);
    }

    @PutMapping("/comentarios/{id}")
    public ModeracionResponse comentario(@PathVariable Long id, @Valid @RequestBody ModeracionRequest datos,
            @AuthenticationPrincipal Identidad identidad) {
        return moderacion.moderarComentario(id, datos, identidad);
    }
}
