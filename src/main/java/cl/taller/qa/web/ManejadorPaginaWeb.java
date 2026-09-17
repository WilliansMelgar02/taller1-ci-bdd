package cl.taller.qa.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Sirve la interfaz web del portal ({@code /}, {@code /app.js}, {@code /estilos.css}).
 *
 * <p>Solo se entregan los archivos de una lista blanca explicita: una ruta
 * construida a partir de la URL podria usarse para leer recursos arbitrarios
 * del classpath ({@code /../}).</p>
 *
 * <p>Los archivos se cargan una vez al construir el manejador. Si alguno
 * faltara en el artefacto, la aplicacion falla al arrancar, que es cuando el
 * health check del despliegue lo detecta, y no en la primera visita de un
 * usuario.</p>
 */
final class ManejadorPaginaWeb implements HttpHandler {

    private final Map<String, Recurso> recursos = new HashMap<>();

    ManejadorPaginaWeb() {
        Recurso pagina = cargar("index.html", "text/html; charset=utf-8");
        recursos.put("/", pagina);
        recursos.put("/index.html", pagina);
        recursos.put("/app.js", cargar("app.js", "text/javascript; charset=utf-8"));
        recursos.put("/estilos.css", cargar("estilos.css", "text/css; charset=utf-8"));
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try {
            Recurso recurso = recursos.get(intercambio.getRequestURI().getPath());
            if (recurso == null) {
                RespuestasHttp.enviarError(intercambio, 404, "Recurso no encontrado");
                return;
            }
            if (!"GET".equals(intercambio.getRequestMethod())) {
                RespuestasHttp.enviarMetodoNoPermitido(intercambio, "GET");
                return;
            }
            // La politica de contenido impide ejecutar scripts de otros origenes (mitiga XSS).
            intercambio.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'");
            RespuestasHttp.enviar(intercambio, 200, recurso.tipo(), recurso.contenido());
        } finally {
            intercambio.close();
        }
    }

    private static Recurso cargar(String nombre, String tipo) {
        String ruta = "/web/" + nombre;
        try (InputStream entrada = ManejadorPaginaWeb.class.getResourceAsStream(ruta)) {
            if (entrada == null) {
                throw new IllegalStateException("Falta el recurso " + ruta + " dentro del artefacto");
            }
            return new Recurso(tipo, entrada.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + ruta, e);
        }
    }

    /**
     * Archivo estatico ya cargado en memoria.
     *
     * @param tipo      valor de Content-Type
     * @param contenido bytes del archivo
     */
    private record Recurso(String tipo, byte[] contenido) {
    }
}
