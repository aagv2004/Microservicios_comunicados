package com.example.foro.servicio_foro.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.foro.servicio_foro.model.Comentario;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    Page<Comentario> findByTemaId(Long temaId, Pageable pageable);
    boolean existsByTemaId(Long temaId);
}
