package cl.taller.qa.aceptacion.soporte;

import java.util.Optional;

/**
 * Datos del ambiente desplegado contra el que corren las pruebas de aceptacion.
 *
 * <p>El pipeline los entrega como propiedades del sistema ({@code -Durl.base},
 * {@code -Dversion.esperada}); asi la misma suite verifica el color green antes
 * de mover el trafico, o el proxy despues, sin cambiar una linea de codigo.</p>
 */
public final class AmbienteBajoPrueba {

    private AmbienteBajoPrueba() {
        // Solo expone metodos estaticos.
    }

    /**
     * URL base de la aplicacion desplegada, sin barra final.
     *
     * @return por ejemplo {@code http://localhost:8082}
     */
    public static String urlBase() {
        String url = leer("url.base").orElse("http://localhost:8080");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * Version que el pipeline acaba de desplegar.
     *
     * <p>Si se entrega, las pruebas confirman que responde exactamente esa
     * version: un despliegue que "funciona" pero sirve la version anterior
     * tambien es un despliegue fallido.</p>
     *
     * @return la version esperada, o vacio si no se indico
     */
    public static Optional<String> versionEsperada() {
        return leer("version.esperada");
    }

    /**
     * Indica si el navegador debe verse en pantalla (util para depurar en local).
     *
     * @return true solo si se paso {@code -Dnavegador.visible=true}
     */
    public static boolean navegadorVisible() {
        return leer("navegador.visible").map(Boolean::parseBoolean).orElse(false);
    }

    private static Optional<String> leer(String propiedad) {
        return Optional.ofNullable(System.getProperty(propiedad))
                .map(String::trim)
                .filter(valor -> !valor.isEmpty());
    }
}
