package cl.taller.qa.web;

import cl.taller.qa.EstadoAutenticacion;
import cl.taller.qa.ResultadoAutenticacion;
import cl.taller.qa.ServicioAutenticacion;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Endpoint {@code POST /api/login}.
 *
 * <p>Capa delgada: traduce HTTP a una llamada del dominio y el resultado del
 * dominio a un codigo HTTP. Las reglas de negocio siguen viviendo en
 * {@link ServicioAutenticacion}, que ya esta cubierto por pruebas unitarias y
 * escenarios BDD; aqui solo se prueba el contrato HTTP.</p>
 */
final class ManejadorLogin implements HttpHandler {

    /** Un login legitimo pesa unos pocos bytes; el limite evita que un cuerpo enorme agote la memoria. */
    static final int TAMANO_MAXIMO_CUERPO = 4_096;

    private final ServicioAutenticacion servicio;

    ManejadorLogin(ServicioAutenticacion servicio) {
        this.servicio = servicio;
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try {
            if (!"POST".equals(intercambio.getRequestMethod())) {
                RespuestasHttp.enviarMetodoNoPermitido(intercambio, "POST");
                return;
            }

            SolicitudLogin solicitud;
            try {
                solicitud = leerSolicitud(intercambio);
            } catch (JsonParseException | IllegalStateException e) {
                RespuestasHttp.enviarError(intercambio, 400, "El cuerpo de la petición no es un JSON válido");
                return;
            }

            ResultadoAutenticacion resultado = servicio.autenticar(solicitud.usuario(), solicitud.contrasena());
            RespuestasHttp.enviarJson(intercambio, codigoHttpPara(resultado.estado()), resultado);
        } finally {
            intercambio.close();
        }
    }

    /**
     * Traduce el desenlace de negocio al codigo HTTP que corresponde.
     *
     * <p>El {@code switch} es exhaustivo: si se agrega un estado nuevo, el
     * codigo deja de compilar hasta que alguien decida su codigo HTTP.</p>
     *
     * @param estado desenlace del intento
     * @return codigo HTTP de la respuesta
     */
    static int codigoHttpPara(EstadoAutenticacion estado) {
        return switch (estado) {
            case CONCEDIDO -> 200;
            case DATOS_INCOMPLETOS -> 400;
            case CREDENCIALES_INVALIDAS -> 401;
            case CUENTA_BLOQUEADA -> 423;
        };
    }

    private static SolicitudLogin leerSolicitud(HttpExchange intercambio) throws IOException {
        String cuerpo;
        try (InputStream entrada = intercambio.getRequestBody()) {
            cuerpo = new String(entrada.readNBytes(TAMANO_MAXIMO_CUERPO), StandardCharsets.UTF_8);
        }
        SolicitudLogin solicitud = RespuestasHttp.GSON.fromJson(cuerpo, SolicitudLogin.class);
        // Un cuerpo vacio no es un error de formato: es una solicitud sin datos (RN-06).
        return solicitud == null ? new SolicitudLogin(null, null) : solicitud;
    }

    /**
     * Cuerpo esperado de la peticion.
     *
     * @param usuario    identificador ingresado
     * @param contrasena clave ingresada
     */
    record SolicitudLogin(String usuario, String contrasena) {
    }
}
