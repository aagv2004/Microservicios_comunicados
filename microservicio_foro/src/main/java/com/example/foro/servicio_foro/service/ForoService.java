package com.example.foro.servicio_foro.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.foro.servicio_foro.dto.*;
import com.example.foro.servicio_foro.exception.ApiException;
import com.example.foro.servicio_foro.model.*;
import com.example.foro.servicio_foro.repository.*;
import com.example.foro.servicio_foro.security.Identidad;

@Service
@Transactional(readOnly = true)
public class ForoService {
    private final CategoriaRepository categorias;
    private final TemaRepository temas;
    private final ComentarioRepository comentarios;

    public ForoService(CategoriaRepository categorias, TemaRepository temas, ComentarioRepository comentarios) {
        this.categorias = categorias;
        this.temas = temas;
        this.comentarios = comentarios;
    }

    public List<CategoriaResponse> listarCategorias() {
        return categorias.findAll(Sort.by("id")).stream().map(CategoriaResponse::de).toList();
    }

    public CategoriaResponse buscarCategoria(Long id) {
        return CategoriaResponse.de(categoria(id));
    }

    @Transactional
    public CategoriaResponse crearCategoria(CategoriaRequest datos, Identidad identidad) {
        exigirAdmin(identidad);
        String nombre = normalizarNombre(datos.nombre());
        if (categorias.existsByNombreNormalizado(nombre)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una categoria con ese nombre");
        }
        var categoria = new Categoria();
        categoria.setNombre(datos.nombre().strip());
        categoria.setNombreNormalizado(nombre);
        categoria.setDescripcion(datos.descripcion().strip());
        return CategoriaResponse.de(categorias.saveAndFlush(categoria));
    }

    @Transactional
    public CategoriaResponse actualizarCategoria(Long id, CategoriaRequest datos, Identidad identidad) {
        exigirAdmin(identidad);
        var categoria = categoria(id);
        String nombre = normalizarNombre(datos.nombre());
        if (categorias.existsByNombreNormalizadoAndIdNot(nombre, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una categoria con ese nombre");
        }
        categoria.setNombre(datos.nombre().strip());
        categoria.setNombreNormalizado(nombre);
        categoria.setDescripcion(datos.descripcion().strip());
        return CategoriaResponse.de(categorias.saveAndFlush(categoria));
    }

    @Transactional
    public void eliminarCategoria(Long id, Identidad identidad) {
        exigirAdmin(identidad);
        var categoria = categoria(id);
        if (temas.existsByCategoriaId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "La categoria tiene temas; reasignalos o eliminalos primero");
        }
        categorias.delete(categoria);
        categorias.flush();
    }

    public PaginaResponse<TemaResponse> buscarTemas(String texto, Long categoriaId, Long autorId,
            int pagina, int tamanio) {
        Pageable pageable = pagina(pagina, tamanio, "creadoEn", true);
        if (texto != null && texto.length() > 150) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La busqueda admite hasta 150 caracteres");
        }
        if (categoriaId != null) categoria(categoriaId);
        if (autorId != null) idPositivo(autorId);
        Specification<Tema> filtros = (root, query, cb) -> {
            var condiciones = new ArrayList<Predicate>();
            if (texto != null && !texto.isBlank()) {
                // Tratar %, _ y la barra como texto, no como comodines del cliente.
                String literal = texto.strip().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                condiciones.add(cb.like(cb.lower(root.get("titulo")), "%" + literal + "%", '\\'));
            }
            if (categoriaId != null) condiciones.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            if (autorId != null) condiciones.add(cb.equal(root.get("autorId"), autorId));
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
        return PaginaResponse.de(temas.findAll(filtros, pageable).map(TemaResponse::de));
    }

    public TemaResponse buscarTema(Long id) {
        return TemaResponse.de(tema(id));
    }

    @Transactional
    public TemaResponse publicar(TemaRequest datos, Identidad identidad) {
        exigirIdentidad(identidad);
        var tema = new Tema();
        tema.setTitulo(datos.titulo().strip());
        tema.setContenido(datos.contenido().strip());
        tema.setCategoria(categoria(datos.categoriaId()));
        tema.setAutorId(identidad.usuarioId());
        return TemaResponse.de(temas.saveAndFlush(tema));
    }

    @Transactional
    public TemaResponse actualizarTema(Long id, TemaRequest datos, Identidad identidad) {
        var tema = tema(id);
        exigirAutor(tema.getAutorId(), identidad);
        tema.setTitulo(datos.titulo().strip());
        tema.setContenido(datos.contenido().strip());
        tema.setCategoria(categoria(datos.categoriaId()));
        return TemaResponse.de(temas.saveAndFlush(tema));
    }

    @Transactional
    public void eliminarTema(Long id, Identidad identidad) {
        var tema = tema(id);
        exigirAutor(tema.getAutorId(), identidad);
        // No borrar silenciosamente los comentarios escritos por otras personas.
        if (comentarios.existsByTemaId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "El tema tiene comentarios; deben eliminarse primero");
        }
        temas.delete(tema);
        temas.flush();
    }

    public PaginaResponse<ComentarioResponse> listarComentarios(Long temaId, int pagina, int tamanio) {
        tema(temaId);
        return PaginaResponse.de(comentarios.findByTemaId(temaId, pagina(pagina, tamanio, "creadoEn", false))
                .map(ComentarioResponse::de));
    }

    public ComentarioResponse buscarComentario(Long id) {
        return ComentarioResponse.de(comentario(id));
    }

    @Transactional
    public ComentarioResponse comentar(Long temaId, ComentarioRequest datos, Identidad identidad) {
        exigirIdentidad(identidad);
        var comentario = new Comentario();
        comentario.setContenido(datos.contenido().strip());
        comentario.setTema(tema(temaId));
        comentario.setAutorId(identidad.usuarioId());
        return ComentarioResponse.de(comentarios.saveAndFlush(comentario));
    }

    @Transactional
    public ComentarioResponse actualizarComentario(Long id, ComentarioRequest datos, Identidad identidad) {
        var comentario = comentario(id);
        exigirAutor(comentario.getAutorId(), identidad);
        comentario.setContenido(datos.contenido().strip());
        return ComentarioResponse.de(comentarios.saveAndFlush(comentario));
    }

    @Transactional
    public void eliminarComentario(Long id, Identidad identidad) {
        var comentario = comentario(id);
        exigirAutor(comentario.getAutorId(), identidad);
        comentarios.delete(comentario);
        comentarios.flush();
    }

    private Categoria categoria(Long id) {
        idPositivo(id);
        return categorias.findById(id).orElseThrow(() -> noEncontrado("Categoria"));
    }

    private Tema tema(Long id) {
        idPositivo(id);
        return temas.findById(id).orElseThrow(() -> noEncontrado("Tema"));
    }

    private Comentario comentario(Long id) {
        idPositivo(id);
        return comentarios.findById(id).orElseThrow(() -> noEncontrado("Comentario"));
    }

    private ApiException noEncontrado(String recurso) {
        return new ApiException(HttpStatus.NOT_FOUND, recurso + " no encontrado");
    }

    private void idPositivo(Long id) {
        if (id == null || id <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "El ID debe ser positivo");
    }

    private Pageable pagina(int pagina, int tamanio, String orden, boolean descendente) {
        if (pagina < 0 || tamanio < 1 || tamanio > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "pagina >= 0 y tamanio entre 1 y 100");
        }
        var sort = descendente ? Sort.by(orden).descending() : Sort.by(orden).ascending();
        return PageRequest.of(pagina, tamanio, sort.and(Sort.by("id")));
    }

    public static String normalizarNombre(String nombre) {
        return nombre.strip().toLowerCase(Locale.ROOT);
    }

    private void exigirIdentidad(Identidad identidad) {
        if (identidad == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Se requiere identidad verificada");
    }

    private void exigirAdmin(Identidad identidad) {
        exigirIdentidad(identidad);
        if (identidad.rol() != Identidad.Rol.ADMIN) throw new AccessDeniedException("Se requiere ADMIN");
    }

    private void exigirAutor(Long autorId, Identidad identidad) {
        exigirIdentidad(identidad);
        if (!autorId.equals(identidad.usuarioId())) throw new AccessDeniedException("Se requiere el autor");
    }
}
