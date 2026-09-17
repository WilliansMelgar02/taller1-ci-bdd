package cl.taller.qa;

/**
 * Desenlace posible de un intento de autenticacion.
 *
 * <p>Se modela como enumeracion y no como combinacion de booleanos para que
 * cada consumidor (la API HTTP, los escenarios BDD, la interfaz web) pueda
 * reaccionar con un {@code switch} exhaustivo: si manana aparece un nuevo
 * desenlace, el compilador obliga a tratarlo en todos lados.</p>
 */
public enum EstadoAutenticacion {

    /** RN-01: usuario y clave coinciden; el acceso se concede. */
    CONCEDIDO,

    /** RN-03: usuario o clave no coinciden; se informa con un mensaje generico. */
    CREDENCIALES_INVALIDAS,

    /** RN-04: la cuenta alcanzo el maximo de intentos fallidos. */
    CUENTA_BLOQUEADA,

    /** RN-06: falta el usuario o la clave; no se descuenta intento. */
    DATOS_INCOMPLETOS
}
