package cl.taller.qa.aceptacion.pasos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import cl.taller.qa.aceptacion.soporte.AmbienteBajoPrueba;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Pasos de aceptacion que verifican el contrato HTTP del servicio desplegado.
 *
 * <p>Complementan a los pasos web: si el Acceptance Gate falla, saber si falla
 * la API o solo la interfaz acorta el diagnostico.</p>
 */
public class PasosApi {

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Gson GSON = new Gson();

    private HttpResponse<String> ultimaRespuesta;

    @Cuando("se consulta el estado de salud del servicio")
    public void consultarSalud() throws IOException, InterruptedException {
        ultimaRespuesta = CLIENTE.send(
                HttpRequest.newBuilder(URI.create(AmbienteBajoPrueba.urlBase() + "/health"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Cuando("se solicita el ingreso por la API con el usuario {string} y la contraseña {string}")
    public void solicitarIngreso(String usuario, String contrasena) throws IOException, InterruptedException {
        String cuerpo = GSON.toJson(Map.of("usuario", usuario, "contrasena", contrasena));
        ultimaRespuesta = CLIENTE.send(
                HttpRequest.newBuilder(URI.create(AmbienteBajoPrueba.urlBase() + "/api/login"))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Cuando("se solicita el ingreso por la API con un usuario no registrado")
    public void solicitarIngresoConUsuarioNoRegistrado() throws IOException, InterruptedException {
        // Usuario distinto en cada ejecucion: repetir la suite no acumula intentos (idempotencia).
        solicitarIngreso("api-" + UUID.randomUUID(), "Clave-Incorrecta-1");
    }

    @Entonces("el servicio responde que está operativo")
    public void verificarOperativo() {
        assertEquals(200, ultimaRespuesta.statusCode(), "El health check no respondio 200");
        assertEquals("UP", cuerpo().get("estado").getAsString());
    }

    @Entonces("el servicio informa la versión que se está desplegando")
    public void verificarVersion() {
        String versionInformada = cuerpo().get("version").getAsString();
        AmbienteBajoPrueba.versionEsperada().ifPresentOrElse(
                versionEsperada -> assertEquals(versionEsperada, versionInformada,
                        "El servicio responde con otra version: el trafico no llega a la version desplegada"),
                () -> assertFalse(versionInformada.isBlank(), "El servicio no informa su version"));
    }

    @Entonces("la API responde con el código {int}")
    public void verificarCodigo(int codigoEsperado) {
        assertEquals(codigoEsperado, ultimaRespuesta.statusCode(),
                "Codigo HTTP inesperado. Cuerpo recibido: " + ultimaRespuesta.body());
    }

    @Entonces("el mensaje de la respuesta es {string}")
    public void verificarMensaje(String mensajeEsperado) {
        assertEquals(mensajeEsperado, cuerpo().get("mensaje").getAsString());
    }

    private JsonObject cuerpo() {
        return JsonParser.parseString(ultimaRespuesta.body()).getAsJsonObject();
    }
}
