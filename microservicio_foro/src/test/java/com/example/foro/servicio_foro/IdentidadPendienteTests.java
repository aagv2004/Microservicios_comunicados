package com.example.foro.servicio_foro;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.example.foro.servicio_foro.repository.TemaRepository;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:pendiente;MODE=Oracle;DB_CLOSE_DELAY=-1")
class IdentidadPendienteTests {
    @Value("${local.server.port}") int port;
    @Autowired TemaRepository temas;

    @Test void modoNormalPermiteLeerPeroNoEscribirSinIntegracion() throws Exception {
        var http = HttpClient.newHttpClient();
        var lectura = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/categorias")).GET().build();
        assertThat(http.send(lectura, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        assertThat(http.send(peticion().header("X-Usuario-Id", "1").build(),
                HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        assertThat(http.send(peticion().header("Authorization", "Basic cHJ1ZWJhOnBydWViYQ==").build(),
                HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(503);
        assertThat(temas.count()).isZero();
    }

    @Test void rechazaCredencialesBasicMalformadas() throws Exception {
        var http = HttpClient.newHttpClient();
        for (String header : new String[] {"Basic abc", "Basic "}) {
            assertThat(http.send(peticion().header("Authorization", header).build(),
                    HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        }
    }

    private HttpRequest.Builder peticion() {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/temas"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"titulo\":\"Prueba\",\"contenido\":\"Texto\",\"categoriaId\":1}"));
    }
}
