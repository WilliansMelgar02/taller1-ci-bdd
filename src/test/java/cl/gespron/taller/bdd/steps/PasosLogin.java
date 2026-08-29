package cl.gespron.taller.bdd.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.gespron.taller.ResultadoAutenticacion;
import cl.gespron.taller.ServicioAutenticacion;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;

/**
 * Definiciones de pasos (step definitions) de la funcionalidad de login.
 *
 * <p><b>Buenas practicas aplicadas</b></p>
 * <ul>
 *   <li><b>Aislamiento por escenario:</b> Cucumber crea una instancia nueva de
 *       esta clase para cada escenario, por lo que {@code servicio} y
 *       {@code ultimoResultado} nacen limpios. Ningun escenario hereda el
 *       estado de otro: los escenarios son atomicos e independientes.</li>
 *   <li><b>Capa delgada:</b> los pasos solo traducen lenguaje de negocio a
 *       llamadas del dominio. La logica vive en {@link ServicioAutenticacion},
 *       no aqui; asi el mismo dominio puede probarse por otras vias.</li>
 *   <li><b>Sin logica condicional:</b> los pasos no contienen if/for que
 *       cambien la expectativa, lo que mantiene el escenario legible y
 *       predecible.</li>
 *   <li><b>Mensajes de asercion explicitos:</b> ante un fallo el reporte indica
 *       que se esperaba, sin necesidad de depurar.</li>
 * </ul>
 */
public class PasosLogin {

    /** Sistema bajo prueba. Se recrea en cada escenario (aislamiento). */
    private final ServicioAutenticacion servicio = new ServicioAutenticacion();

    /** Ultimo resultado devuelto por el servicio, compartido entre pasos del mismo escenario. */
    private ResultadoAutenticacion ultimoResultado;

    // ------------------------------------------------------------------
    // DADO - preparacion del contexto
    // ------------------------------------------------------------------

    @Dado("que el portal tiene registrado al cliente {string} con la contraseña {string}")
    public void registrarCliente(String usuario, String contrasena) {
        servicio.registrarUsuario(usuario, contrasena);
    }

    // ------------------------------------------------------------------
    // CUANDO - accion que ejecuta el usuario
    // ------------------------------------------------------------------

    @Cuando("el cliente intenta ingresar con el usuario {string} y la contraseña {string}")
    public void intentarIngresar(String usuario, String contrasena) {
        ultimoResultado = servicio.autenticar(usuario, contrasena);
    }

    /**
     * Reutilizado tanto con la palabra clave "Dado" como con "Cuando": Cucumber
     * empareja el texto del paso, no la palabra clave, lo que evita duplicar
     * definiciones.
     */
    @Cuando("el cliente falla el ingreso {int} veces seguidas")
    @Dado("que el cliente falla el ingreso {int} veces seguidas")
    public void fallarIngresoVarias(int cantidadIntentos) {
        for (int intento = 1; intento <= cantidadIntentos; intento++) {
            ultimoResultado = servicio.autenticar("wmelgar", "clave-incorrecta-" + intento);
        }
    }

    // ------------------------------------------------------------------
    // ENTONCES - verificacion del resultado esperado
    // ------------------------------------------------------------------

    @Entonces("el acceso es concedido")
    public void verificarAccesoConcedido() {
        assertNotNull(ultimoResultado, "No se ejecuto ningun intento de ingreso");
        assertTrue(ultimoResultado.exitoso(),
                "Se esperaba acceso concedido, pero el sistema respondio: " + ultimoResultado.mensaje());
    }

    @Entonces("el acceso es denegado")
    public void verificarAccesoDenegado() {
        assertNotNull(ultimoResultado, "No se ejecuto ningun intento de ingreso");
        assertFalse(ultimoResultado.exitoso(),
                "Se esperaba acceso denegado, pero el ingreso fue exitoso");
    }

    @Entonces("el sistema muestra el mensaje {string}")
    public void verificarMensaje(String mensajeEsperado) {
        assertEquals(mensajeEsperado, ultimoResultado.mensaje(),
                "El mensaje mostrado al usuario no corresponde al acordado con Negocio");
    }

    @Entonces("le quedan {int} intentos disponibles")
    public void verificarIntentosRestantes(int intentosEsperados) {
        assertEquals(intentosEsperados, ultimoResultado.intentosRestantes(),
                "El contador de intentos restantes no coincide con la regla RN-04");
    }

    @Entonces("la cuenta del cliente {string} queda bloqueada")
    public void verificarCuentaBloqueada(String usuario) {
        assertTrue(servicio.estaBloqueada(usuario),
                "La cuenta debia quedar bloqueada tras alcanzar el maximo de intentos");
    }
}
