package com.example.foro.servicio_usuarios.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.foro.servicio_usuarios.dto.*;
import com.example.foro.servicio_usuarios.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public UsuariosPagina listar(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        var resultado = usuarios.listar(pagina, tamanio);
        return new UsuariosPagina(resultado.getContent(), resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id, Authentication auth) {
        return usuarios.buscar(id, auth);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest datos,
            Authentication auth) {
        return usuarios.actualizar(id, datos, auth);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> cambiarPassword(@PathVariable Long id,
            @Valid @RequestBody CambiarPasswordRequest datos, Authentication auth) {
        usuarios.cambiarPassword(id, datos, auth);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        usuarios.eliminar(id, auth);
        return ResponseEntity.noContent().build();
    }

    public record UsuariosPagina(java.util.List<UsuarioResponse> usuarios, int pagina, int tamanio,
            long totalElementos, int totalPaginas) {
    }
}
