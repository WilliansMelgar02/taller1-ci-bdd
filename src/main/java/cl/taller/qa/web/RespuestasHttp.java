package cl.taller.qa.web;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Utilidades para escribir respuestas HTTP de forma uniforme.
 *
 * <p>Centralizar la escritura garantiza que todos los endpoints devuelvan las
 * mismas cabeceras de seguridad y el mismo formato de error, en lugar de que
 * cada manejador lo resuelva a su manera.</p>
 */
final class RespuestasHttp {

    static final String TIPO_JSON = "application/json; charset=utf-8";

    /** Gson es inmutable y seguro para hilos: una sola instancia sirve a todas las peticiones. */
    static final Gson GSON = new Gson();

    private RespuestasHttp() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Serializa el cuerpo como JSON y lo envia con el codigo indicado.
     *
     * @param intercambio peticion en curso
     * @param estado      codigo HTTP de la respuesta
     * @param cuerpo      objeto a serializar
     * @throws IOException si la conexion se cierra durante la escritura
     */
    static void enviarJson(HttpExchange intercambio, int estado, Object cuerpo) throws IOException {
        enviar(intercambio, estado, TIPO_JSON, GSON.toJson(cuerpo).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Envia un error con el mismo formato que usan las respuestas de negocio.
     *
     * @param intercambio peticion en curso
     * @param estado      codigo HTTP de la respuesta
     * @param mensaje     texto legible para el consumidor de la API
     * @throws IOException si la conexion se cierra durante la escritura
     */
    static void enviarError(HttpExchange intercambio, int estado, String mensaje) throws IOException {
        enviarJson(intercambio, estado, Map.of("mensaje", mensaje));
    }

    /**
     * Responde 405 indicando en la cabecera {@code Allow} el unico metodo aceptado.
     *
     * @param intercambio     peticion en curso
     * @param metodoPermitido metodo HTTP que si acepta el recurso
     * @throws IOException si la conexion se cierra durante la escritura
     */
    static void enviarMetodoNoPermitido(HttpExchange intercambio, String metodoPermitido) throws IOException {
        intercambio.getResponseHeaders().set("Allow", metodoPermitido);
        enviarError(intercambio, 405, "Método no permitido; use " + metodoPermitido);
    }

    /**
     * Envia bytes con el tipo de contenido indicado y cierra el intercambio.
     *
     * @param intercambio peticion en curso
     * @param estado      codigo HTTP de la respuesta
     * @param tipo        valor de la cabecera Content-Type
     * @param cuerpo      contenido de la respuesta
     * @throws IOException si la conexion se cierra durante la escritura
     */
    static void enviar(HttpExchange intercambio, int estado, String tipo, byte[] cuerpo) throws IOException {
        var cabeceras = intercambio.getResponseHeaders();
        cabeceras.set("Content-Type", tipo);
        cabeceras.set("X-Content-Type-Options", "nosniff");
        cabeceras.set("Cache-Control", "no-store");
        intercambio.sendResponseHeaders(estado, cuerpo.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        }
    }
}
