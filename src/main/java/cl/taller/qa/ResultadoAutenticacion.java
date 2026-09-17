package cl.taller.qa;

/**
 * Resultado inmutable de un intento de autenticacion.
 *
 * <p>Se modela como {@code record} para que sea un objeto de valor: sin
 * setters, sin estado mutable y comparable por contenido. Devolver un objeto
 * de valor en lugar de un simple boolean permite que las pruebas verifiquen
 * el mensaje y los intentos restantes sin acceder a la implementacion
 * interna del servicio (bajo acoplamiento).</p>
 *
 * <p>El mismo objeto viaja serializado como cuerpo JSON de la API, por lo que
 * sus componentes forman parte del contrato publico del servicio.</p>
 *
 * @param estado            desenlace del intento
 * @param mensaje           texto que la interfaz muestra al usuario
 * @param intentosRestantes intentos disponibles antes del bloqueo
 */
public record ResultadoAutenticacion(
        EstadoAutenticacion estado,
        String mensaje,
        int intentosRestantes) {

    /**
     * Indica si el acceso fue concedido.
     *
     * @return true si las credenciales fueron validadas
     */
    public boolean exitoso() {
        return estado == EstadoAutenticacion.CONCEDIDO;
    }

    /**
     * Indica si el intento termino con la cuenta bloqueada.
     *
     * @return true si la cuenta quedo bloqueada
     */
    public boolean cuentaBloqueada() {
        return estado == EstadoAutenticacion.CUENTA_BLOQUEADA;
    }
}
