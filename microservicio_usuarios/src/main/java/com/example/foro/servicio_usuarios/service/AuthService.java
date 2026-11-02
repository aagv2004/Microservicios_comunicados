package com.example.foro.servicio_usuarios.service;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import com.example.foro.servicio_usuarios.dto.*;
import com.example.foro.servicio_usuarios.exception.ApiException;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarios;

    public AuthService(AuthenticationManager authenticationManager, UsuarioService usuarios) {
        this.authenticationManager = authenticationManager;
        this.usuarios = usuarios;
    }

    public LoginResponse login(LoginRequest datos) {
        if (datos.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Correo o password incorrectos");
        }
        try {
            var authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(datos.correo(), datos.password()));
            return new LoginResponse("Inicio de sesion correcto",
                    usuarios.buscar(Long.valueOf(authentication.getName()), authentication));
        } catch (AuthenticationException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Correo o password incorrectos");
        }
    }
}
