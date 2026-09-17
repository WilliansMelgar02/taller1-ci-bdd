package cl.taller.qa.integracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.taller.qa.ServicioAutenticacion;
import cl.taller.qa.web.ConfiguracionPortal;
import cl.taller.qa.web.ServidorPortal;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de INTEGRACION de la API del portal.
 *
 * <p>A diferencia de las pruebas unitarias, aqui no se aisla ninguna pieza:
 * se levanta el servidor HTTP real y se le habla por la red, de modo que en
 * cada prueba participan juntos el enrutamiento, la lectura del cuerpo, la
 * serializacion JSON, el servicio de autenticacion y la traduccion a codigos
 * HTTP. Un defecto en la union entre esas piezas (por ejemplo, un campo JSON
 * mal nombrado) pasa todas las pruebas unitarias y solo se detecta aqui.</p>
 *
 * <p><b>Independencia:</b> cada prueba levanta su propio servidor en un puerto
 * libre (puerto 0) con un servicio nuevo. Ninguna prueba hereda intentos
 * fallidos ni cuentas bloqueadas de otra, y pueden correr en cualquier orden.</p>
 */
@DisplayName("Integracion - API HTTP del Portal de Clientes")
class ApiPortalIT {

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private ServidorPortal servidor;
    private String urlBase;

    @BeforeEach
    void levantarServidor() throws IOException {
        ConfiguracionPortal configuracion = new ConfiguracionPortal(
                0, "9.9.9-integracion", "abc1234def", "green", Map.of());

        ServicioAutenticacion servicio = new ServicioAutenticacion();
        servicio.registrarUsuario("wmelgar", "Segura2026!");

        servidor = ServidorPortal.iniciar(configuracion, servicio);
        urlBase = "http://localhost:" + servidor.puerto();
    }

    @AfterEach
    void detenerServidor() {
        servidor.close();
    }

    @Nested
    @DisplayName("POST /api/login")
    class Login {

        @Test
        @DisplayName("Credenciales validas responden 200 con el mensaje de bienvenida")
        void credencialesValidas() throws Exception {
            HttpResponse<String> respuesta = login("{\"usuario\":\"wmelgar\",\"contrasena\":\"Segura2026!\"}");

            assertEquals(200, respuesta.statusCode());
            assertTrue(respuesta.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
            JsonObject cuerpo = json(respuesta);
            assertEquals("CONCEDIDO", cuerpo.get("estado").getAsString());
            assertEquals("Bienvenido/a, wmelgar", cuerpo.get("mensaje").getAsString());
            assertEquals(3, cuerpo.get("intentosRestantes").getAsInt());
        }

        @Test
        @DisplayName("Una contrasena incorrecta responde 401 y descuenta un intento")
        void contrasenaIncorrecta() throws Exception {
            HttpResponse<String> respuesta = login("{\"usuario\":\"wmelgar\",\"contrasena\":\"1234\"}");

            assertEquals(401, respuesta.statusCode());
            JsonObject cuerpo = json(respuesta);
            assertEquals("Credenciales inválidas", cuerpo.get("mensaje").getAsString(),
                    "Los acentos deben llegar intactos: la respuesta se codifica en UTF-8");
            assertEquals(2, cuerpo.get("intentosRestantes").getAsInt());
        }

        @Test
        @DisplayName("El tercer intento fallido responde 423 y el estado se conserva entre peticiones")
        void bloqueoTrasTresIntentos() throws Exception {
            login("{\"usuario\":\"wmelgar\",\"contrasena\":\"mala-1\"}");
            login("{\"usuario\":\"wmelgar\",\"contrasena\":\"mala-2\"}");
            HttpResponse<String> tercerIntento = login("{\"usuario\":\"wmelgar\",\"contrasena\":\"mala-3\"}");

            assertEquals(423, tercerIntento.statusCode());
            assertEquals("CUENTA_BLOQUEADA", json(tercerIntento).get("estado").getAsString());

            HttpResponse<String> conClaveCorrecta = login("{\"usuario\":\"wmelgar\",\"contrasena\":\"Segura2026!\"}");
            assertEquals(423, conClaveCorrecta.statusCode(),
                    "Una cuenta bloqueada no debe aceptar ni la contrasena correcta");
        }

        @Test
        @DisplayName("Campos vacios responden 400 sin descontar intentos")
        void camposVacios() throws Exception {
            HttpResponse<String> respuesta = login("{\"usuario\":\"wmelgar\",\"contrasena\":\"\"}");

            assertEquals(400, respuesta.statusCode());
            assertEquals("DATOS_INCOMPLETOS", json(respuesta).get("estado").getAsString());
            assertEquals(3, json(respuesta).get("intentosRestantes").getAsInt());
        }

        @Test
        @DisplayName("Un campo con otro nombre se trata como dato faltante (contrato JSON)")
        void campoMalNombrado() throws Exception {
            // Protege el contrato con la interfaz web: si alguien renombra
            // 'contrasena' en un lado y no en el otro, esta prueba lo evidencia.
            HttpResponse<String> respuesta = login("{\"usuario\":\"wmelgar\",\"clave\":\"Segura2026!\"}");

            assertEquals(400, respuesta.statusCode());
        }

        @Test
        @DisplayName("Un cuerpo que no es JSON responde 400 con un mensaje claro")
        void cuerpoNoJson() throws Exception {
            HttpResponse<String> respuesta = login("usuario=wmelgar&contrasena=Segura2026!");

            assertEquals(400, respuesta.statusCode());
            assertEquals("El cuerpo de la petición no es un JSON válido", json(respuesta).get("mensaje").getAsString());
        }

        @Test
        @DisplayName("GET sobre el login responde 405 e informa el metodo permitido")
        void metodoNoPermitido() throws Exception {
            HttpResponse<String> respuesta = get("/api/login");

            assertEquals(405, respuesta.statusCode());
            assertEquals("POST", respuesta.headers().firstValue("Allow").orElse(""));
        }
    }

    @Nested
    @DisplayName("GET /health")
    class Salud {

        @Test
        @DisplayName("Informa estado, version, commit y color del despliegue")
        void informaVersionDesplegada() throws Exception {
            HttpResponse<String> respuesta = get("/health");

            assertEquals(200, respuesta.statusCode());
            JsonObject cuerpo = json(respuesta);
            assertEquals("UP", cuerpo.get("estado").getAsString());
            assertEquals("9.9.9-integracion", cuerpo.get("version").getAsString());
            assertEquals("abc1234def", cuerpo.get("commit").getAsString());
            assertEquals("green", cuerpo.get("color").getAsString());
        }
    }

    @Nested
    @DisplayName("Interfaz web")
    class InterfazWeb {

        @Test
        @DisplayName("La raiz entrega el formulario de ingreso con politica de seguridad de contenido")
        void entregaFormulario() throws Exception {
            HttpResponse<String> respuesta = get("/");

            assertEquals(200, respuesta.statusCode());
            assertTrue(respuesta.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));
            assertEquals("default-src 'self'", respuesta.headers().firstValue("Content-Security-Policy").orElse(""));
            assertTrue(respuesta.body().contains("id=\"formulario-login\""), "Falta el formulario de ingreso");
        }

        @Test
        @DisplayName("El script de la pagina usa los mismos campos que espera la API")
        void scriptRespetaContratoDeLaApi() throws Exception {
            HttpResponse<String> respuesta = get("/app.js");

            assertEquals(200, respuesta.statusCode());
            assertTrue(respuesta.body().contains("'/api/login'"), "El script debe invocar la API de login");
        }

        @Test
        @DisplayName("Una ruta desconocida responde 404 en formato JSON")
        void rutaDesconocida() throws Exception {
            HttpResponse<String> respuesta = get("/../pom.xml");

            assertEquals(404, respuesta.statusCode());
            assertEquals("Recurso no encontrado", json(respuesta).get("mensaje").getAsString());
        }
    }

    private HttpResponse<String> login(String cuerpoJson) throws IOException, InterruptedException {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlBase + "/api/login"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpoJson))
                .build();
        return CLIENTE.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String ruta) throws IOException, InterruptedException {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlBase + ruta))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return CLIENTE.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> respuesta) {
        return JsonParser.parseString(respuesta.body()).getAsJsonObject();
    }
}
