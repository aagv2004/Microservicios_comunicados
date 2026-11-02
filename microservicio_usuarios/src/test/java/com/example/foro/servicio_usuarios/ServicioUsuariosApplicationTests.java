package com.example.foro.servicio_usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.example.foro.servicio_usuarios.model.Rol;
import com.example.foro.servicio_usuarios.model.Usuario;
import com.example.foro.servicio_usuarios.repository.UsuarioRepository;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServicioUsuariosApplicationTests {
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String NUEVA_PASSWORD = UUID.randomUUID().toString();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    int port;
    @Autowired
    UsuarioRepository repository;
    @Autowired
    PasswordEncoder encoder;

    private Long adminId;
    private Long usuarioId;
    private Long otroId;

    @BeforeEach
    void preparar() {
        repository.deleteAll();
        String hash = encoder.encode(PASSWORD);
        adminId = crear("admin@foro.test", Rol.ADMIN, hash);
        usuarioId = crear("usuario1@foro.test", Rol.USUARIO, hash);
        otroId = crear("usuario2@foro.test", Rol.USUARIO, hash);
    }

    @Test
    void registroNormalizaCorreoProtegePasswordYEvitaDuplicados() throws Exception {
        var respuesta = enviar("POST", "/api/auth/registro", registro("NUEVO@FORO.TEST"), null);
        assertThat(respuesta.statusCode()).isEqualTo(201);
        assertThat(respuesta.headers().firstValue("Location")).isPresent();
        var body = json(respuesta);
        assertThat(body.get("rol").asText()).isEqualTo("USUARIO");
        assertThat(body.get("correo").asText()).isEqualTo("nuevo@foro.test");
        assertSinSecretos(respuesta);
        var guardado = repository.findByCorreo("nuevo@foro.test").orElseThrow();
        assertThat(guardado.getNombre()).isEqualTo("Nuevo Usuario");
        assertThat(guardado.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, guardado.getPasswordHash())).isTrue();
        assertThat(enviar("POST", "/api/auth/registro", registro("nuevo@foro.test"), null).statusCode())
                .isEqualTo(409);
    }

    @Test
    void rechazaDatosInvalidosCamposDesconocidosYEscaladaDeRol() throws Exception {
        var invalido = enviar("POST", "/api/auth/registro",
                Map.of("nombre", " ", "correo", "sin-correo", "password", "corta"), null);
        assertThat(invalido.statusCode()).isEqualTo(400);
        assertThat(json(invalido).get("campos").size()).isEqualTo(3);
        assertThat(enviar("POST", "/api/auth/registro",
                Map.of("nombre", "Intruso", "correo", "intruso@foro.test", "password", PASSWORD,
                        "rol", "ADMIN"), null).statusCode()).isEqualTo(400);
        assertThat(repository.findByCorreo("intruso@foro.test")).isEmpty();
        assertThat(enviar("POST", "/api/auth/registro",
                Map.of("nombre", "Nuevo", "correo", "unicode@foro.test", "password", "ñ".repeat(40)), null)
                .statusCode()).isEqualTo(400);
    }

    @Test
    void loginYPerfilSinExponerSecretos() throws Exception {
        var login = enviar("POST", "/api/auth/login", loginDatos("usuario1@foro.test", PASSWORD), null);
        assertThat(login.statusCode()).isEqualTo(200);
        assertSinSecretos(login);
        assertThat(json(login).get("accessToken")).isNull();
        assertThat(json(login).get("usuario").get("id").asLong()).isEqualTo(usuarioId);
        var perfil = enviar("GET", "/api/auth/me", null, credenciales("usuario1@foro.test"));
        assertThat(perfil.statusCode()).isEqualTo(200);
        assertThat(json(perfil).get("id").asLong()).isEqualTo(usuarioId);
        assertSinSecretos(perfil);
    }

    @Test
    void loginIncorrectoNoDistingueCorreoInexistenteDePasswordErronea() throws Exception {
        var incorrecto = enviar("POST", "/api/auth/login", loginDatos("usuario1@foro.test", "incorrecta"), null);
        var inexistente = enviar("POST", "/api/auth/login", loginDatos("nadie@foro.test", "incorrecta"), null);
        assertThat(incorrecto.statusCode()).isEqualTo(401);
        assertThat(inexistente.statusCode()).isEqualTo(401);
        assertThat(json(incorrecto).get("mensaje").asText()).isEqualTo(json(inexistente).get("mensaje").asText());
    }

    @Test
    void exigeCredencialesYRechazaPasswordIncorrecta() throws Exception {
        assertThat(enviar("GET", "/api/auth/me", null, null).statusCode()).isEqualTo(401);
        assertThat(enviar("GET", "/api/auth/me", null, "invalido").statusCode()).isEqualTo(401);
        String incorrectas = Base64.getEncoder().encodeToString(
                "usuario1@foro.test:incorrecta".getBytes(StandardCharsets.UTF_8));
        assertThat(enviar("GET", "/api/auth/me", null, incorrectas).statusCode()).isEqualTo(401);
    }

    @Test
    void usuarioNoPuedeListarNiGestionarOtrasCuentas() throws Exception {
        String credencial = credenciales("usuario1@foro.test");
        assertThat(enviar("GET", "/api/usuarios", null, credencial).statusCode()).isEqualTo(403);
        assertThat(enviar("GET", "/api/usuarios/" + otroId, null, credencial).statusCode()).isEqualTo(403);
        assertThat(enviar("PUT", "/api/usuarios/" + otroId, actualizar("ajeno@foro.test"), credencial)
                .statusCode()).isEqualTo(403);
        assertThat(enviar("DELETE", "/api/usuarios/" + otroId, null, credencial).statusCode()).isEqualTo(403);
        assertThat(repository.existsById(otroId)).isTrue();
    }

    @Test
    void adminPuedeListarConsultarActualizarYEliminarUnUsuario() throws Exception {
        String admin = credenciales("admin@foro.test");
        var listado = enviar("GET", "/api/usuarios?pagina=0&tamanio=2", null, admin);
        assertThat(listado.statusCode()).isEqualTo(200);
        assertThat(json(listado).get("usuarios").size()).isEqualTo(2);
        assertThat(json(listado).get("totalElementos").asLong()).isEqualTo(3);
        assertSinSecretos(listado);
        assertThat(enviar("GET", "/api/usuarios/" + usuarioId, null, admin).statusCode()).isEqualTo(200);
        assertThat(enviar("PUT", "/api/usuarios/" + usuarioId, actualizar("editado@foro.test"), admin)
                .statusCode()).isEqualTo(200);
        assertThat(repository.findById(usuarioId).orElseThrow().getCorreo()).isEqualTo("editado@foro.test");
        var borrado = enviar("DELETE", "/api/usuarios/" + usuarioId, null, admin);
        assertThat(borrado.statusCode()).isEqualTo(204);
        assertThat(borrado.body()).isEmpty();
        assertThat(repository.existsById(usuarioId)).isFalse();
        assertThat(enviar("GET", "/api/usuarios/" + usuarioId, null, admin).statusCode()).isEqualTo(404);
    }

    @Test
    void propietarioPuedeActualizarSuCuentaPeroNoAsignarseRolNiRepetirCorreo() throws Exception {
        String credencial = credenciales("usuario1@foro.test");
        assertThat(enviar("PUT", "/api/usuarios/" + usuarioId, actualizar("USUARIO2@FORO.TEST"), credencial)
                .statusCode()).isEqualTo(409);
        assertThat(enviar("PUT", "/api/usuarios/" + usuarioId,
                Map.of("nombre", "Editado", "correo", "editado@foro.test", "rol", "ADMIN"), credencial)
                .statusCode()).isEqualTo(400);
        assertThat(enviar("PUT", "/api/usuarios/" + usuarioId, actualizar("EDITADO@FORO.TEST"), credencial)
                .statusCode()).isEqualTo(200);
        assertThat(repository.findById(usuarioId).orElseThrow().getRol()).isEqualTo(Rol.USUARIO);
        assertThat(enviar("POST", "/api/auth/login", loginDatos("editado@foro.test", PASSWORD), null)
                .statusCode()).isEqualTo(200);
    }

    @Test
    void cambioDePasswordExigePasswordActualYRechazaCredencialesAnteriores() throws Exception {
        String credencial = credenciales("usuario1@foro.test");
        String ruta = "/api/usuarios/" + usuarioId + "/password";
        assertThat(enviar("PUT", ruta, Map.of("passwordActual", "incorrecta", "passwordNueva", NUEVA_PASSWORD),
                credencial).statusCode()).isEqualTo(400);
        assertThat(enviar("PUT", ruta, Map.of("passwordActual", PASSWORD, "passwordNueva", NUEVA_PASSWORD),
                credencial).statusCode()).isEqualTo(204);
        assertThat(enviar("GET", "/api/auth/me", null, credencial).statusCode()).isEqualTo(401);
        assertThat(enviar("POST", "/api/auth/login", loginDatos("usuario1@foro.test", PASSWORD), null)
                .statusCode()).isEqualTo(401);
        assertThat(enviar("POST", "/api/auth/login", loginDatos("usuario1@foro.test", NUEVA_PASSWORD), null)
                .statusCode()).isEqualTo(200);
        String admin = credenciales("admin@foro.test");
        assertThat(enviar("PUT", ruta, Map.of("passwordActual", NUEVA_PASSWORD, "passwordNueva", PASSWORD), admin)
                .statusCode()).isEqualTo(403);
    }

    @Test
    void eliminarPropiaCuentaImpideAutenticacionYProtegeAdministradores() throws Exception {
        String usuario = credenciales("usuario1@foro.test");
        assertThat(enviar("DELETE", "/api/usuarios/" + usuarioId, null, usuario).statusCode()).isEqualTo(204);
        assertThat(enviar("GET", "/api/auth/me", null, usuario).statusCode()).isEqualTo(401);
        String admin = credenciales("admin@foro.test");
        assertThat(enviar("DELETE", "/api/usuarios/" + adminId, null, admin).statusCode()).isEqualTo(409);
    }

    @Test
    void validaParametrosYPaginacion() throws Exception {
        String admin = credenciales("admin@foro.test");
        assertThat(enviar("GET", "/api/usuarios/abc", null, admin).statusCode()).isEqualTo(400);
        assertThat(enviar("GET", "/api/usuarios/0", null, admin).statusCode()).isEqualTo(400);
        assertThat(enviar("GET", "/api/usuarios?pagina=-1", null, admin).statusCode()).isEqualTo(400);
        assertThat(enviar("GET", "/api/usuarios?tamanio=101", null, admin).statusCode()).isEqualTo(400);
    }

    @Test
    void rechazaJsonMalformadoYPasswordUnicodeDemasiadoLargaSinErrorInterno() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/registro"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{json-invalido"))
                .build();
        assertThat(HTTP.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);
        assertThat(enviar("POST", "/api/auth/login", loginDatos("usuario1@foro.test", "ñ".repeat(40)), null)
                .statusCode()).isEqualTo(401);
    }

    @Test
    void metodosNoPermitidosResponden405() throws Exception {
        String admin = credenciales("admin@foro.test");
        assertThat(enviar("POST", "/api/usuarios/" + usuarioId, Map.of(), admin).statusCode()).isEqualTo(405);
    }

    private Long crear(String correo, Rol rol, String hash) {
        var usuario = new Usuario();
        usuario.setNombre("Cuenta de prueba");
        usuario.setCorreo(correo);
        usuario.setPasswordHash(hash);
        usuario.setRol(rol);
        return repository.saveAndFlush(usuario).getId();
    }

    private Map<String, String> registro(String correo) {
        return Map.of("nombre", "  Nuevo Usuario  ", "correo", correo, "password", PASSWORD);
    }

    private Map<String, String> actualizar(String correo) {
        return Map.of("nombre", "Usuario Editado", "correo", correo);
    }

    private Map<String, String> loginDatos(String correo, String password) {
        return Map.of("correo", correo, "password", password);
    }

    private String credenciales(String correo) {
        return Base64.getEncoder().encodeToString((correo + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> enviar(String metodo, String ruta, Object datos, String credencial) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + ruta))
                .header("Content-Type", "application/json");
        if (credencial != null) {
            request.header("Authorization", "Basic " + credencial);
        }
        request.method(metodo, datos == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(datos)));
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> respuesta) {
        return JSON.readTree(respuesta.body());
    }

    private void assertSinSecretos(HttpResponse<String> respuesta) {
        assertThat(respuesta.body()).doesNotContain("passwordHash", "password_hash", PASSWORD, "$2a$", "$2b$");
    }
}
