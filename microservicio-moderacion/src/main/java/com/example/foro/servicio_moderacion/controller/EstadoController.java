package com.example.foro.servicio_moderacion.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.foro.servicio_moderacion.dto.EstadoResponse;
import com.example.foro.servicio_moderacion.service.EstadoService;

@RestController
public class EstadoController {
    private final EstadoService estados;

    public EstadoController(EstadoService estados) { this.estados = estados; }

    @GetMapping("/api/estado")
    public EstadoResponse obtener() { return estados.obtener(); }
}
