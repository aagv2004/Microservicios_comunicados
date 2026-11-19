package com.example.foro.servicio_moderacion.service;

import org.springframework.stereotype.Service;
import com.example.foro.servicio_moderacion.dto.EstadoResponse;

@Service
public class EstadoService {
    public EstadoResponse obtener() {
        return new EstadoResponse("microservicio-moderacion", "OK");
    }
}
