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
 * @param exitoso           true si las credenciales fueron validadas
 * @param mensaje           texto que la interfaz muestra al usuario
 * @param intentosRestantes intentos disponibles antes del bloqueo
 * @param cuentaBloqueada   true si la cuenta quedo bloqueada
 */
public record ResultadoAutenticacion(
        boolean exitoso,
        String mensaje,
        int intentosRestantes,
        boolean cuentaBloqueada) {
}
