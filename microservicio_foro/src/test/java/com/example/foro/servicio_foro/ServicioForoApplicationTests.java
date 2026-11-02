package com.example.foro.servicio_foro;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.net.http.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.example.foro.servicio_foro.dto.*;
import com.example.foro.servicio_foro.exception.ApiException;
import com.example.foro.servicio_foro.repository.*;
import com.example.foro.servicio_foro.security.*;
import com.example.foro.servicio_foro.service.ForoService;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ServicioForoApplicationTests.IdentidadesDePrueba.class)
class ServicioForoApplicationTests {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Identidad ADMIN = new Identidad(100L, Identidad.Rol.ADMIN);
    private static final Identidad AUTOR = new Identidad(101L, Identidad.Rol.USUARIO);
    private static final Identidad OTRO = new Identidad(102L, Identidad.Rol.USUARIO);
    @Value("${local.server.port}") int port;
    @Autowired ForoService service;
    @Autowired CategoriaRepository categorias;
    @Autowired TemaRepository temas;
    @Autowired ComentarioRepository comentarios;
    private Long categoriaId;
    private Long otraCategoriaId;
    private Long temaId;

    @TestConfiguration(proxyBeanMethods = false)
    static class IdentidadesDePrueba {
        // Verificador exclusivo de src/test: no conecta con Usuarios.
        @Bean @Primary
        VerificadorIdentidad identidadTest() {
            return (correo, password) -> {
                if (!"prueba".equals(password)) {
                    throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales de prueba incorrectas");
                }
                return switch (correo) {
                case "test-admin" -> ADMIN;
                case "test-autor" -> AUTOR;
                case "test-otro" -> OTRO;
                default -> throw new ApiException(HttpStatus.UNAUTHORIZED, "Cuenta de prueba desconocida");
                };
            };
        }
    }

    @BeforeEach
    void preparar() {
        comentarios.deleteAll(); temas.deleteAll(); categorias.deleteAll();
        categoriaId = service.crearCategoria(new CategoriaRequest("Programacion", "Software"), ADMIN).id();
        otraCategoriaId = service.crearCategoria(new CategoriaRequest("Videojuegos", "Juegos"), ADMIN).id();
        for (String nombre : List.of("Tecnologia", "Cine", "Musica")) {
            service.crearCategoria(new CategoriaRequest(nombre, "Conversaciones de " + nombre), ADMIN);
        }
        temaId = service.publicar(new TemaRequest("Aprender Spring Boot", "Contenido inicial", categoriaId), AUTOR).id();
    }

    @Test void lecturaPublicaConCincoCategorias() throws Exception {
        var respuesta = enviar("GET", "/api/categorias", null, null);
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(json(respuesta).size()).isEqualTo(5);
        assertThat(enviar("GET", "/api/categorias/" + categoriaId, null, null).statusCode()).isEqualTo(200);
    }

    @Test void crudCategoriasSoloParaAdminYNombreUnico() throws Exception {
        var datos = Map.of("nombre", "Ciencia", "descripcion", "Experimentos");
        assertThat(enviar("POST", "/api/categorias", datos, "test-autor").statusCode()).isEqualTo(403);
        var alta = enviar("POST", "/api/categorias", datos, "test-admin");
        assertThat(alta.statusCode()).isEqualTo(201);
        assertThat(alta.headers().firstValue("Location")).isPresent();
        long id = json(alta).get("id").asLong();
        assertThat(enviar("POST", "/api/categorias", Map.of("nombre", " CIENCIA ", "descripcion", "Otra"),
                "test-admin").statusCode()).isEqualTo(409);
        assertThat(enviar("PUT", "/api/categorias/" + id, Map.of("nombre", "Astronomia", "descripcion", "Estrellas"),
                "test-admin").statusCode()).isEqualTo(200);
        assertThat(categorias.findById(id).orElseThrow().getNombreNormalizado()).isEqualTo("astronomia");
        assertThat(enviar("DELETE", "/api/categorias/" + id, null, "test-admin").statusCode()).isEqualTo(204);
        assertThat(categorias.existsById(id)).isFalse();
    }

    @Test void categoriaConTemasNoSeElimina() throws Exception {
        assertThat(enviar("DELETE", "/api/categorias/" + categoriaId, null, "test-admin").statusCode()).isEqualTo(409);
        assertThat(categorias.existsById(categoriaId)).isTrue();
    }

    @Test void publicarYActualizarAsignaAutorDesdeIdentidad() throws Exception {
        var alta = enviar("POST", "/api/temas", temaDatos("Mi tema", categoriaId), "test-autor");
        assertThat(alta.statusCode()).isEqualTo(201);
        long id = json(alta).get("id").asLong();
        assertThat(json(alta).get("autorId").asLong()).isEqualTo(AUTOR.usuarioId());
        assertThat(json(alta).get("creadoEn").asText()).endsWith("Z");
        var cambio = enviar("PUT", "/api/temas/" + id, temaDatos("Editado", otraCategoriaId), "test-autor");
        assertThat(cambio.statusCode()).isEqualTo(200);
        assertThat(json(cambio).get("categoriaId").asLong()).isEqualTo(otraCategoriaId);
        assertThat(temas.findById(id).orElseThrow().getTitulo()).isEqualTo("Editado");
        assertThat(enviar("GET", "/api/temas/" + id, null, null).statusCode()).isEqualTo(200);
        assertThat(enviar("POST", "/api/temas", Map.of("titulo", "Fraude", "contenido", "Texto",
                "categoriaId", categoriaId, "autorId", 999), "test-autor").statusCode()).isEqualTo(400);
    }

    @Test void otroUsuarioYAdminNoModificanTemaAjeno() throws Exception {
        for (String correo : List.of("test-otro", "test-admin")) {
            assertThat(enviar("PUT", "/api/temas/" + temaId, temaDatos("Ajeno", categoriaId), correo)
                    .statusCode()).isEqualTo(403);
            assertThat(enviar("DELETE", "/api/temas/" + temaId, null, correo).statusCode()).isEqualTo(403);
        }
    }

    @Test void autorBorraTemaSinComentarios() throws Exception {
        var borrado = enviar("DELETE", "/api/temas/" + temaId, null, "test-autor");
        assertThat(borrado.statusCode()).isEqualTo(204);
        assertThat(borrado.body()).isEmpty();
        assertThat(temas.existsById(temaId)).isFalse();
        assertThat(enviar("GET", "/api/temas/" + temaId, null, null).statusCode()).isEqualTo(404);
    }

    @Test void crudComentarios() throws Exception {
        var alta = enviar("POST", "/api/temas/" + temaId + "/comentarios", Map.of("contenido", "Comentario"), "test-otro");
        assertThat(alta.statusCode()).isEqualTo(201);
        long id = json(alta).get("id").asLong();
        assertThat(json(alta).get("autorId").asLong()).isEqualTo(OTRO.usuarioId());
        assertThat(enviar("GET", "/api/comentarios/" + id, null, null).statusCode()).isEqualTo(200);
        var lista = enviar("GET", "/api/temas/" + temaId + "/comentarios", null, null);
        assertThat(json(lista).get("totalElementos").asLong()).isEqualTo(1);
        assertThat(enviar("PUT", "/api/comentarios/" + id, Map.of("contenido", "Editado"), "test-otro")
                .statusCode()).isEqualTo(200);
        assertThat(comentarios.findById(id).orElseThrow().getContenido()).isEqualTo("Editado");
        assertThat(enviar("DELETE", "/api/comentarios/" + id, null, "test-otro").statusCode()).isEqualTo(204);
        assertThat(enviar("GET", "/api/comentarios/" + id, null, null).statusCode()).isEqualTo(404);
    }

    @Test void protegeComentariosAjenosYBloqueaBorradoDeTemaConRespuestas() throws Exception {
        long id = service.comentar(temaId, new ComentarioRequest("Comentario ajeno"), OTRO).id();
        assertThat(enviar("PUT", "/api/comentarios/" + id, Map.of("contenido", "Cambio"), "test-autor")
                .statusCode()).isEqualTo(403);
        assertThat(enviar("DELETE", "/api/comentarios/" + id, null, "test-admin").statusCode()).isEqualTo(403);
        assertThat(enviar("DELETE", "/api/temas/" + temaId, null, "test-autor").statusCode()).isEqualTo(409);
        assertThat(comentarios.existsById(id)).isTrue();
        assertThat(enviar("DELETE", "/api/comentarios/" + id, null, "test-otro").statusCode()).isEqualTo(204);
        assertThat(enviar("DELETE", "/api/temas/" + temaId, null, "test-autor").statusCode()).isEqualTo(204);
    }

    @Test void busquedaCombinadaYPaginacion() throws Exception {
        service.publicar(new TemaRequest("Java avanzado", "Otro", categoriaId), OTRO);
        service.publicar(new TemaRequest("Spring y videojuegos", "Otro", otraCategoriaId), AUTOR);
        var filtrado = enviar("GET", "/api/temas?texto=SPRING&categoriaId=" + categoriaId + "&autorId=101", null, null);
        assertThat(filtrado.statusCode()).isEqualTo(200);
        assertThat(json(filtrado).get("totalElementos").asLong()).isEqualTo(1);
        assertThat(json(filtrado).get("elementos").get(0).get("id").asLong()).isEqualTo(temaId);
        var pagina = enviar("GET", "/api/temas?pagina=0&tamanio=2", null, null);
        assertThat(json(pagina).get("elementos").size()).isEqualTo(2);
        assertThat(json(pagina).get("totalElementos").asLong()).isEqualTo(3);
    }

    @Test void comodinesDeBusquedaSeTratanComoTexto() throws Exception {
        service.publicar(new TemaRequest("100% de avance", "Texto", categoriaId), AUTOR);
        service.publicar(new TemaRequest("variable_con_guion", "Texto", categoriaId), AUTOR);
        assertThat(json(enviar("GET", "/api/temas?texto=%25", null, null)).get("totalElementos").asLong()).isEqualTo(1);
        assertThat(json(enviar("GET", "/api/temas?texto=_", null, null)).get("totalElementos").asLong()).isEqualTo(1);
    }

    @Test void validaEntradasYRechazaCamposDesconocidos() throws Exception {
        var invalido = enviar("POST", "/api/temas", Map.of("titulo", "", "contenido", " ", "categoriaId", -1), "test-autor");
        assertThat(invalido.statusCode()).isEqualTo(400);
        assertThat(json(invalido).get("campos").size()).isEqualTo(3);
        assertThat(enviar("POST", "/api/temas/" + temaId + "/comentarios", Map.of("contenido", ""), "test-autor")
                .statusCode()).isEqualTo(400);
        assertThat(enviar("POST", "/api/temas/" + temaId + "/comentarios",
                Map.of("contenido", "Texto", "autorId", 999), "test-autor").statusCode()).isEqualTo(400);
        assertThat(enviar("POST", "/api/categorias", Map.of("nombre", "", "descripcion", ""), "test-admin")
                .statusCode()).isEqualTo(400);
        assertThat(enviar("POST", "/api/temas", Map.of("titulo", "Tema", "contenido", "x".repeat(10001),
                "categoriaId", categoriaId), "test-autor").statusCode()).isEqualTo(400);
    }

    @Test void compruebaRelacionesYRecursosInexistentes() throws Exception {
        assertThat(enviar("POST", "/api/temas", temaDatos("Sin categoria", 999999L), "test-autor").statusCode()).isEqualTo(404);
        assertThat(enviar("POST", "/api/temas/999999/comentarios", Map.of("contenido", "Texto"), "test-autor")
                .statusCode()).isEqualTo(404);
        for (String ruta : List.of("/api/temas/999999", "/api/categorias/999999", "/api/comentarios/999999")) {
            assertThat(enviar("GET", ruta, null, null).statusCode()).isEqualTo(404);
        }
    }

    @Test void idsYPaginacionInvalidosDevuelven400() throws Exception {
        for (String ruta : List.of("/api/temas/0", "/api/temas/no-numero", "/api/categorias/-1",
                "/api/temas?pagina=-1", "/api/temas?tamanio=101", "/api/temas?autorId=-1",
                "/api/temas/" + temaId + "/comentarios?tamanio=0")) {
            assertThat(enviar("GET", ruta, null, null).statusCode()).as(ruta).isEqualTo(400);
        }
    }

    @Test void exigeCredencialesYRechazaCuentaDesconocida() throws Exception {
        assertThat(enviar("POST", "/api/temas", temaDatos("Sin correo", categoriaId), null).statusCode()).isEqualTo(401);
        assertThat(enviar("POST", "/api/temas", temaDatos("Malo", categoriaId), "desconocido").statusCode()).isEqualTo(401);
    }

    @Test void contenidoUnicodeLargoSeAlmacenaComoClob() throws Exception {
        String texto = "🙂".repeat(4000);
        var alta = enviar("POST", "/api/temas", Map.of("titulo", "Unicode", "contenido", texto,
                "categoriaId", categoriaId), "test-autor");
        assertThat(alta.statusCode()).isEqualTo(201);
        assertThat(json(alta).get("contenido").asText()).isEqualTo(texto);
    }

    @Test void jsonYMetodoIncorrectosNoDevuelven500() throws Exception {
        assertThat(enviar("POST", "/api/temas/" + temaId, Map.of(), "test-autor").statusCode()).isEqualTo(405);
        var peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/temas"))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                        "test-autor:prueba".getBytes(StandardCharsets.UTF_8)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{malformado")).build();
        assertThat(HTTP.send(peticion, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);
    }

    private Map<String, Object> temaDatos(String titulo, Long categoria) {
        return Map.of("titulo", titulo, "contenido", "Contenido de prueba", "categoriaId", categoria);
    }

    private HttpResponse<String> enviar(String metodo, String ruta, Object datos, String correo) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + ruta)).header("Content-Type", "application/json");
        if (correo != null) request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (correo + ":prueba").getBytes(StandardCharsets.UTF_8)));
        request.method(metodo, datos == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(datos)));
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> respuesta) {
        return JSON.readTree(respuesta.body());
    }
}
