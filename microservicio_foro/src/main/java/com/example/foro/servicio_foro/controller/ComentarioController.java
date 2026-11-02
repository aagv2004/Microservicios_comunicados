package com.example.foro.servicio_foro.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.foro.servicio_foro.dto.*;
import com.example.foro.servicio_foro.security.Identidad;
import com.example.foro.servicio_foro.service.ForoService;

@RestController
@RequestMapping("/api/comentarios")
public class ComentarioController {
    private final ForoService foro;

    public ComentarioController(ForoService foro) {
        this.foro = foro;
    }

    @GetMapping("/{id}")
    public ComentarioResponse buscar(@PathVariable Long id) {
        return foro.buscarComentario(id);
    }

    @PutMapping("/{id}")
    public ComentarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ComentarioRequest datos,
            @AuthenticationPrincipal Identidad identidad) {
        return foro.actualizarComentario(id, datos, identidad);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal Identidad identidad) {
        foro.eliminarComentario(id, identidad);
        return ResponseEntity.noContent().build();
    }
}
