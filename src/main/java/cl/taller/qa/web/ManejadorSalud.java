package cl.taller.qa.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

/**
 * Endpoint {@code GET /health}.
 *
 * <p>Lo consumen tres actores distintos, y por eso informa mas que un simple
 * "estoy vivo":</p>
 * <ul>
 *   <li>el {@code HEALTHCHECK} de Docker, para saber si el contenedor esta sano;</li>
 *   <li>el pipeline de despliegue, que antes de mover trafico confirma que
 *       responde la <b>version esperada</b> y no una anterior;</li>
 *   <li>el rollback, que verifica que el trafico volvio al color estable.</li>
 * </ul>
 */
final class ManejadorSalud implements HttpHandler {

    private final EstadoSalud estadoSalud;

    ManejadorSalud(ConfiguracionPortal configuracion) {
        this.estadoSalud = new EstadoSalud(
                "UP", configuracion.version(), configuracion.commit(), configuracion.color());
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try {
            if (!"GET".equals(intercambio.getRequestMethod())) {
                RespuestasHttp.enviarMetodoNoPermitido(intercambio, "GET");
                return;
            }
            RespuestasHttp.enviarJson(intercambio, 200, estadoSalud);
        } finally {
            intercambio.close();
        }
    }

    /**
     * Cuerpo de la respuesta de salud.
     *
     * @param estado  "UP" mientras el proceso atiende peticiones
     * @param version version desplegada
     * @param commit  commit de origen del artefacto
     * @param color   color Blue-Green del contenedor
     */
    record EstadoSalud(String estado, String version, String commit, String color) {
    }
}
