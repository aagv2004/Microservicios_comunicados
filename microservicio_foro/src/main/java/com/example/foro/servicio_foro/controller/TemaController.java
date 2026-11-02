package com.example.foro.servicio_foro.controller;

import java.net.URI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.foro.servicio_foro.dto.*;
import com.example.foro.servicio_foro.security.Identidad;
import com.example.foro.servicio_foro.service.ForoService;

@RestController
@RequestMapping("/api/temas")
public class TemaController {
    private final ForoService foro;

    public TemaController(ForoService foro) {
        this.foro = foro;
    }

    @GetMapping
    public PaginaResponse<TemaResponse> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long autorId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return foro.buscarTemas(texto, categoriaId, autorId, pagina, tamanio);
    }

    @GetMapping("/{id}")
    public TemaResponse obtener(@PathVariable Long id) {
        return foro.buscarTema(id);
    }

    @PostMapping
    public ResponseEntity<TemaResponse> publicar(@Valid @RequestBody TemaRequest datos,
            @AuthenticationPrincipal Identidad identidad) {
        var tema = foro.publicar(datos, identidad);
        return ResponseEntity.created(URI.create("/api/temas/" + tema.id())).body(tema);
    }

    @PutMapping("/{id}")
    public TemaResponse actualizar(@PathVariable Long id, @Valid @RequestBody TemaRequest datos,
            @AuthenticationPrincipal Identidad identidad) {
        return foro.actualizarTema(id, datos, identidad);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal Identidad identidad) {
        foro.eliminarTema(id, identidad);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{temaId}/comentarios")
    public PaginaResponse<ComentarioResponse> comentarios(@PathVariable Long temaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return foro.listarComentarios(temaId, pagina, tamanio);
    }

    @PostMapping("/{temaId}/comentarios")
    public ResponseEntity<ComentarioResponse> comentar(@PathVariable Long temaId,
            @Valid @RequestBody ComentarioRequest datos, @AuthenticationPrincipal Identidad identidad) {
        var comentario = foro.comentar(temaId, datos, identidad);
        return ResponseEntity.created(URI.create("/api/comentarios/" + comentario.id())).body(comentario);
    }
}
