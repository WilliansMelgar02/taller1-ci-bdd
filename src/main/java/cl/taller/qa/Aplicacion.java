package cl.taller.qa;

import cl.taller.qa.web.ConfiguracionPortal;
import cl.taller.qa.web.ServidorPortal;
import java.io.IOException;

/**
 * Punto de entrada del Portal de Clientes ({@code java -jar portal-clientes.jar}).
 *
 * <p>Solo ensambla las piezas: lee la configuracion, registra los usuarios de
 * demostracion y levanta el servidor. No contiene logica propia, por lo que
 * se excluye del umbral de cobertura; su correcto funcionamiento lo verifican
 * el health check y las pruebas de aceptacion sobre el contenedor desplegado.</p>
 */
public final class Aplicacion {

    private Aplicacion() {
        // Solo expone main.
    }

    /**
     * Arranca el portal y lo mantiene activo hasta que el proceso reciba SIGTERM.
     *
     * @param argumentos no se usan: toda la configuracion llega por variables de entorno
     * @throws IOException si el puerto configurado no esta disponible
     */
    public static void main(String[] argumentos) throws IOException {
        ConfiguracionPortal configuracion = ConfiguracionPortal.desdeVariables(System.getenv());

        ServicioAutenticacion servicio = new ServicioAutenticacion();
        configuracion.usuariosIniciales().forEach(servicio::registrarUsuario);

        ServidorPortal servidor = ServidorPortal.iniciar(configuracion, servicio);

        // 'docker stop' envia SIGTERM: se cierra el servidor de forma ordenada
        // en lugar de cortar peticiones a medio responder.
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::close, "detencion-portal"));

        System.out.printf("Portal de Clientes %s (commit %s, color %s) escuchando en el puerto %d%n",
                configuracion.version(), configuracion.commit(), configuracion.color(), servidor.puerto());
    }
}
