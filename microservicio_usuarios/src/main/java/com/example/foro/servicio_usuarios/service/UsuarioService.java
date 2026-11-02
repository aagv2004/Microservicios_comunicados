package com.example.foro.servicio_usuarios.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.foro.servicio_usuarios.dto.*;
import com.example.foro.servicio_usuarios.exception.ApiException;
import com.example.foro.servicio_usuarios.model.Rol;
import com.example.foro.servicio_usuarios.model.Usuario;
import com.example.foro.servicio_usuarios.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest datos) {
        String correo = normalizarCorreo(datos.correo());
        if (repository.existsByCorreo(correo)) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya esta registrado");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(datos.nombre().strip());
        usuario.setCorreo(correo);
        usuario.setPasswordHash(codificarPassword(datos.password()));
        // Nunca aceptar el rol desde el registro publico.
        usuario.setRol(Rol.USUARIO);
        return UsuarioResponse.de(repository.saveAndFlush(usuario));
    }

    public Page<UsuarioResponse> listar(int pagina, int tamanio) {
        if (pagina < 0 || tamanio < 1 || tamanio > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "pagina >= 0 y tamanio entre 1 y 100");
        }
        return repository.findAll(PageRequest.of(pagina, tamanio, Sort.by("id")))
                .map(UsuarioResponse::de);
    }

    public UsuarioResponse buscar(Long id, Authentication auth) {
        exigirPropietarioOAdmin(id, auth);
        return UsuarioResponse.de(obtener(id));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest datos, Authentication auth) {
        exigirPropietarioOAdmin(id, auth);
        Usuario usuario = obtener(id);
        String correo = normalizarCorreo(datos.correo());
        if (repository.existsByCorreoAndIdNot(correo, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya esta registrado");
        }
        usuario.setNombre(datos.nombre().strip());
        usuario.setCorreo(correo);
        return UsuarioResponse.de(repository.saveAndFlush(usuario));
    }

    @Transactional
    public void cambiarPassword(Long id, CambiarPasswordRequest datos, Authentication auth) {
        if (!id.toString().equals(auth.getName())) {
            throw new AccessDeniedException("Solo puedes cambiar tu propia password");
        }
        Usuario usuario = obtener(id);
        if (!passwordEncoder.matches(datos.passwordActual(), usuario.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La password actual es incorrecta");
        }
        usuario.setPasswordHash(codificarPassword(datos.passwordNueva()));
        repository.saveAndFlush(usuario);
    }

    @Transactional
    public void eliminar(Long id, Authentication auth) {
        exigirPropietarioOAdmin(id, auth);
        Usuario usuario = obtener(id);
        // Las cuentas ADMIN se conservan para evitar perder el acceso administrativo.
        if (usuario.getRol() == Rol.ADMIN) {
            throw new ApiException(HttpStatus.CONFLICT, "No se permite eliminar cuentas ADMIN");
        }
        repository.delete(usuario);
        repository.flush();
    }

    private Usuario obtener(Long id) {
        if (id <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El id debe ser positivo");
        }
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private void exigirPropietarioOAdmin(Long id, Authentication auth) {
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin && !id.toString().equals(auth.getName())) {
            throw new AccessDeniedException("No puedes acceder a otra cuenta");
        }
    }

    public static String normalizarCorreo(String correo) {
        return correo.strip().toLowerCase(Locale.ROOT);
    }

    private String codificarPassword(String password) {
        // BCrypt admite hasta 72 bytes; caracteres Unicode pueden ocupar varios bytes.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La password no puede superar 72 bytes UTF-8");
        }
        return passwordEncoder.encode(password);
    }
}
