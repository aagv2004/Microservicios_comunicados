package com.example.foro.servicio_moderacion;

import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ModeracionIntegrationTests {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final HttpServer UPSTREAM = iniciarServidor();
    private static final AtomicInteger llamadasForo = new AtomicInteger();
    private static int estadoUsuarios = 200;
    private static int estadoForo = 200;
    private static String forwardedAuth;
    private static String forwardedPath;
    private static String forwardedBody;
    @LocalServerPort int port;

    @DynamicPropertySource
    static void direcciones(DynamicPropertyRegistry registry) {
        String url = "http://localhost:" + UPSTREAM.getAddress().getPort();
        registry.add("usuarios.base-url", () -> url);
        registry.add("foro.base-url", () -> url);
    }

    private static HttpServer iniciarServidor() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/api/auth/me", exchange -> {
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                int status = estadoUsuarios;
                String role = auth("usuario").equals(auth) ? "USUARIO" : "ADMIN";
                if (!auth("admin").equals(auth) && !auth("usuario").equals(auth)) status = 401;
                byte[] body = ("{\"id\":100,\"nombre\":\"Prueba\",\"correo\":\"prueba\",\"rol\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, body.length);
                try (var stream = exchange.getResponseBody()) { stream.write(body); }
            });
            server.createContext("/api/moderacion", exchange -> {
                llamadasForo.incrementAndGet();
                forwardedAuth = exchange.getRequestHeaders().getFirst("Authorization");
                forwardedPath = exchange.getRequestURI().getPath();
                forwardedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                byte[] body = "{\"tipo\":\"TEMA\",\"id\":1,\"bloqueado\":true,\"motivo\":\"Spam\",\"moderadorId\":100}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(estadoForo, body.length);
                try (var stream = exchange.getResponseBody()) { stream.write(body); }
            });
            server.start();
            return server;
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    @BeforeEach void preparar() {
        llamadasForo.set(0); estadoUsuarios = 200; estadoForo = 200;
        forwardedAuth = null; forwardedBody = null; forwardedPath = null;
    }
    @AfterAll static void cerrar() { UPSTREAM.stop(0); }

    @Test void adminValidaIdentidadYReenviaOperacionYCredenciales() throws Exception {
        var response = enviar("PUT", "/api/moderacion/temas/1", datos(true), "admin");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(response.body()).get("moderadorId").asLong()).isEqualTo(100);
        assertThat(forwardedPath).isEqualTo("/api/moderacion/temas/1");
        assertThat(forwardedAuth).isEqualTo(auth("admin"));
        assertThat(JSON.readTree(forwardedBody).get("bloqueado").asBoolean()).isTrue();
    }

    @Test void permiteRestaurarComentariosConMismoContrato() throws Exception {
        assertThat(enviar("PUT", "/api/moderacion/comentarios/7", datos(false), "admin").statusCode()).isEqualTo(200);
        assertThat(forwardedPath).isEqualTo("/api/moderacion/comentarios/7");
        assertThat(JSON.readTree(forwardedBody).get("bloqueado").asBoolean()).isFalse();
    }

    @Test void anonimoUsuarioYCredencialInvalidaNoLleganAForo() throws Exception {
        assertThat(enviar("PUT", "/api/moderacion/temas/1", datos(true), null).statusCode()).isEqualTo(401);
        assertThat(enviar("PUT", "/api/moderacion/temas/1", datos(true), "usuario").statusCode()).isEqualTo(403);
        assertThat(enviar("PUT", "/api/moderacion/temas/1", datos(true), "invalido").statusCode()).isEqualTo(401);
        assertThat(llamadasForo.get()).isZero();
    }

    @Test void validaCamposYNoPermiteAsignarModeradorDesdeCliente() throws Exception {
        for (var body : java.util.List.of(Map.of("motivo", "Sin estado"),
                Map.of("bloqueado", true, "motivo", " "),
                Map.of("bloqueado", true, "motivo", "x".repeat(501)),
                Map.of("bloqueado", true, "motivo", "Spam", "moderadorId", 999))) {
            assertThat(enviar("PUT", "/api/moderacion/temas/1", body, "admin").statusCode()).isEqualTo(400);
        }
        assertThat(enviar("PUT", "/api/moderacion/temas/0", datos(true), "admin").statusCode()).isEqualTo(400);
        assertThat(llamadasForo.get()).isZero();
    }

    @Test void conservaErroresEsperadosDeForoSinExponerSuCuerpo() throws Exception {
        for (int status : new int[] {400, 401, 403, 404, 409}) {
            estadoForo = status;
            var response = enviar("PUT", "/api/moderacion/temas/1", datos(true), "admin");
            assertThat(response.statusCode()).isEqualTo(status);
            assertThat(response.body()).doesNotContain("moderadorId");
        }
    }

    @Test void fallosDeServiciosExternosDevuelven503() throws Exception {
        estadoUsuarios = 503;
        assertThat(enviar("PUT", "/api/moderacion/temas/1", datos(true), "admin").statusCode()).isEqualTo(503);
        assertThat(llamadasForo.get()).isZero();
        estadoUsuarios = 200; estadoForo = 500;
        assertThat(enviar("PUT", "/api/moderacion/temas/1", datos(true), "admin").statusCode()).isEqualTo(503);
    }

    @Test void unGetNoEvitaLaAutorizacion() throws Exception {
        assertThat(enviar("GET", "/api/moderacion/temas/1", null, null).statusCode()).isEqualTo(401);
        assertThat(enviar("GET", "/api/moderacion/temas/1", null, "usuario").statusCode()).isEqualTo(403);
        assertThat(llamadasForo.get()).isZero();
    }

    private static Map<String,Object> datos(boolean bloqueado) { return Map.of("bloqueado", bloqueado, "motivo", "Spam"); }
    private static String auth(String usuario) {
        return "Basic " + Base64.getEncoder().encodeToString((usuario + ":prueba").getBytes(StandardCharsets.UTF_8));
    }
    private HttpResponse<String> enviar(String method, String path, Object body, String usuario) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (usuario != null) request.header("Authorization", auth(usuario));
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
