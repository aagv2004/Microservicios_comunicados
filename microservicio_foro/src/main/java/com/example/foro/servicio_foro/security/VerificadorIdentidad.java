package com.example.foro.servicio_foro.security;

/** Verifica las credenciales y obtiene la identidad desde el servicio de Usuarios. */
public interface VerificadorIdentidad {
    Identidad verificar(String correo, String password);
}
