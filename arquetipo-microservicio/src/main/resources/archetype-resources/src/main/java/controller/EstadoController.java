package ${package}.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ${package}.dto.EstadoResponse;
import ${package}.service.EstadoService;

@RestController
public class EstadoController {
    private final EstadoService estados;

    public EstadoController(EstadoService estados) { this.estados = estados; }

    @GetMapping("/api/estado")
    public EstadoResponse obtener() { return estados.obtener(); }
}
