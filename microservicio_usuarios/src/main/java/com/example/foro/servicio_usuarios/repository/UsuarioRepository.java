package com.example.foro.servicio_usuarios.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.foro.servicio_usuarios.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Long id);
}
