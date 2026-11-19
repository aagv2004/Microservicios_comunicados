package ${package};

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MicroservicioApplicationTests {
    @LocalServerPort private int port;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void estadoPublicoDevuelveJsonConLaIdentidadGenerada() throws Exception {
        var response = get("/api/estado");
        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("").contains("application/json"));
        assertTrue(response.body().contains("\"servicio\":\"${artifactId}\""));
        assertTrue(response.body().contains("\"estado\":\"OK\""));
    }

    @Test
    void otrasRutasNoQuedanExpuestasPorDefecto() throws Exception {
        assertEquals(403, get("/api/recurso-privado").statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
