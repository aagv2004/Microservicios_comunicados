package com.example.foro.servicio_usuarios.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.foro.servicio_usuarios.dto.*;
import com.example.foro.servicio_usuarios.service.AuthService;
import com.example.foro.servicio_usuarios.service.UsuarioService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioService usuarios;
    private final AuthService authService;

    public AuthController(UsuarioService usuarios, AuthService authService) {
        this.usuarios = usuarios;
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest datos) {
        var usuario = usuarios.registrar(datos);
        return ResponseEntity.created(URI.create("/api/usuarios/" + usuario.id())).body(usuario);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest datos) {
        return authService.login(datos);
    }

    @GetMapping("/me")
    public UsuarioResponse actual(Authentication auth) {
        return usuarios.buscar(Long.valueOf(auth.getName()), auth);
    }
}
