package com.example.foro.servicio_usuarios.dto;

import com.example.foro.servicio_usuarios.model.Rol;
import com.example.foro.servicio_usuarios.model.Usuario;

public record UsuarioResponse(Long id, String nombre, String correo, Rol rol) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol());
    }
}
