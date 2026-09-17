package cl.taller.qa.web;

import cl.taller.qa.ServicioAutenticacion;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP del Portal de Clientes.
 *
 * <p>Usa el servidor HTTP incluido en el JDK en lugar de un framework: la
 * aplicacion expone tres rutas, arranca en milisegundos (lo que acorta el
 * health check del despliegue Blue-Green) y la imagen Docker queda liviana.</p>
 *
 * <p>Implementa {@link AutoCloseable} para que las pruebas de integracion lo
 * levanten y lo detengan con {@code try-with-resources}, sin dejar puertos
 * ocupados entre una prueba y otra.</p>
 */
public final class ServidorPortal implements AutoCloseable {

    /** Hilos que atienden peticiones en paralelo; suficiente para la carga del SLA (10 VUs). */
    static final int HILOS_DE_ATENCION = 16;

    private final HttpServer servidor;
    private final ExecutorService hilos;

    private ServidorPortal(HttpServer servidor, ExecutorService hilos) {
        this.servidor = servidor;
        this.hilos = hilos;
    }

    /**
     * Levanta el servidor y registra las rutas del portal.
     *
     * @param configuracion puerto, version y color del despliegue
     * @param servicio      servicio de autenticacion ya poblado con usuarios
     * @return servidor escuchando peticiones
     * @throws IOException si el puerto no esta disponible
     */
    public static ServidorPortal iniciar(ConfiguracionPortal configuracion, ServicioAutenticacion servicio)
            throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(configuracion.puerto()), 0);
        servidor.createContext("/api/login", new ManejadorLogin(servicio));
        servidor.createContext("/health", new ManejadorSalud(configuracion));
        servidor.createContext("/", new ManejadorPaginaWeb());

        ExecutorService hilos = Executors.newFixedThreadPool(HILOS_DE_ATENCION);
        servidor.setExecutor(hilos);
        servidor.start();
        return new ServidorPortal(servidor, hilos);
    }

    /**
     * Puerto real de escucha. Difiere del configurado cuando se pidio el puerto 0.
     *
     * @return puerto TCP asignado
     */
    public int puerto() {
        return servidor.getAddress().getPort();
    }

    /** Deja de aceptar conexiones y libera los hilos de atencion. */
    @Override
    public void close() {
        servidor.stop(0);
        hilos.shutdownNow();
    }
}
