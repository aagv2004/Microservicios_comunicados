package com.example.foro.servicio_foro.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.foro.servicio_foro.dto.*;
import com.example.foro.servicio_foro.security.Identidad;
import com.example.foro.servicio_foro.service.ForoService;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {
    private final ForoService foro;

    public CategoriaController(ForoService foro) {
        this.foro = foro;
    }

    @GetMapping
    public List<CategoriaResponse> listar() {
        return foro.listarCategorias();
    }

    @GetMapping("/{id}")
    public CategoriaResponse buscar(@PathVariable Long id) {
        return foro.buscarCategoria(id);
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest datos,
            @AuthenticationPrincipal Identidad identidad) {
        var categoria = foro.crearCategoria(datos, identidad);
        return ResponseEntity.created(URI.create("/api/categorias/" + categoria.id())).body(categoria);
    }

    @PutMapping("/{id}")
    public CategoriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest datos,
            @AuthenticationPrincipal Identidad identidad) {
        return foro.actualizarCategoria(id, datos, identidad);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal Identidad identidad) {
        foro.eliminarCategoria(id, identidad);
        return ResponseEntity.noContent().build();
    }
}
