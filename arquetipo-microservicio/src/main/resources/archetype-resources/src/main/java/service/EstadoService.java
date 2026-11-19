package ${package}.service;

import org.springframework.stereotype.Service;
import ${package}.dto.EstadoResponse;

@Service
public class EstadoService {
    public EstadoResponse obtener() {
        return new EstadoResponse("${artifactId}", "OK");
    }
}
