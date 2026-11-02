package com.example.foro.servicio_foro.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.foro.servicio_foro.model.Tema;

public interface TemaRepository extends JpaRepository<Tema, Long>, JpaSpecificationExecutor<Tema> {
    boolean existsByCategoriaId(Long categoriaId);

    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Tema> findAll(Specification<Tema> spec, Pageable pageable);
}
