package com.example.foro.servicio_moderacion.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.example.foro.servicio_moderacion.dto.*;
import com.example.foro.servicio_moderacion.service.ModeracionService;

@RestController
@RequestMapping("/api/moderacion")
public class ModeracionController {
    private final ModeracionService moderacion;
    public ModeracionController(ModeracionService moderacion) { this.moderacion = moderacion; }

    @PutMapping("/temas/{id}")
    public ModeracionResponse tema(@PathVariable Long id, @Valid @RequestBody ModeracionRequest datos,
            @RequestHeader("Authorization") String authorization) {
        return moderacion.moderar("temas", id, datos, authorization);
    }

    @PutMapping("/comentarios/{id}")
    public ModeracionResponse comentario(@PathVariable Long id, @Valid @RequestBody ModeracionRequest datos,
            @RequestHeader("Authorization") String authorization) {
        return moderacion.moderar("comentarios", id, datos, authorization);
    }
}
