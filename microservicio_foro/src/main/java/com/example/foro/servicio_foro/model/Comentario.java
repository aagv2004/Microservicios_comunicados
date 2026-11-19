package com.example.foro.servicio_foro.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "foro_comentarios")
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Lob
    @Column(nullable = false)
    private String contenido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tema_id", nullable = false)
    private Tema tema;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
    @Column(nullable = false)
    private boolean bloqueado;

    @Column(name = "motivo_bloqueo", length = 500)
    private String motivoBloqueo;

    @Column(name = "moderador_id")
    private Long moderadorId;

    public boolean isBloqueado() { return bloqueado; }
    public String getMotivoBloqueo() { return motivoBloqueo; }
    public Long getModeradorId() { return moderadorId; }

    public void moderar(boolean bloqueado, String motivo, Long moderadorId) {
        this.bloqueado = bloqueado;
        this.motivoBloqueo = motivo;
        this.moderadorId = moderadorId;
    }

    @PrePersist
    void alCrear() {
        creadoEn = Instant.now();
        actualizadoEn = creadoEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = Instant.now();
    }

    public Comentario() {
    }

    public Long getId() {
        return id;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Tema getTema() {
        return tema;
    }

    public void setTema(Tema tema) {
        this.tema = tema;
    }

    public Long getAutorId() {
        return autorId;
    }

    public void setAutorId(Long autorId) {
        this.autorId = autorId;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
