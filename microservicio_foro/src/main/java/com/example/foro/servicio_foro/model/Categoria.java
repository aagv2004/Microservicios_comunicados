package com.example.foro.servicio_foro.model;

import jakarta.persistence.*;

@Entity
@Table(name = "foro_categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "nombre_normalizado", nullable = false, unique = true, length = 60)
    private String nombreNormalizado;

    @Column(nullable = false, length = 250)
    private String descripcion;

    public Categoria() {
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNombreNormalizado() {
        return nombreNormalizado;
    }

    public void setNombreNormalizado(String nombreNormalizado) {
        this.nombreNormalizado = nombreNormalizado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
