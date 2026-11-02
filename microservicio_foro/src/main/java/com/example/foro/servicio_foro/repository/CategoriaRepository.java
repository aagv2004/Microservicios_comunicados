package com.example.foro.servicio_foro.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.foro.servicio_foro.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByNombreNormalizado(String nombreNormalizado);
    boolean existsByNombreNormalizadoAndIdNot(String nombreNormalizado, Long id);
    Optional<Categoria> findByNombreNormalizado(String nombreNormalizado);
}
