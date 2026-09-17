package cl.taller.qa.aceptacion.pasos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import cl.taller.qa.aceptacion.soporte.AmbienteBajoPrueba;
import cl.taller.qa.aceptacion.soporte.FabricaNavegador;
import cl.taller.qa.aceptacion.soporte.PaginaLogin;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import java.util.UUID;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Pasos de aceptacion que recorren el portal con un navegador real (Selenium).
 *
 * <p><b>Idempotencia:</b> estas pruebas corren contra un ambiente compartido
 * que no se reinicia entre escenarios. Por eso ningun escenario deja estado que
 * afecte a otro o a una segunda ejecucion: los intentos fallidos usan un
 * usuario inventado distinto cada vez, y los ingresos correctos reinician el
 * contador del cliente (RN-05).</p>
 */
public class PasosPortalWeb {

    private WebDriver navegador;
    private PaginaLogin paginaLogin;

    /** Solo los escenarios web abren navegador: los de API no pagan ese costo. */
    @Before("@web")
    public void abrirNavegador() {
        navegador = FabricaNavegador.abrirChrome();
        paginaLogin = new PaginaLogin(navegador);
    }

    /**
     * Si el escenario fallo, adjunta una captura al reporte antes de cerrar.
     * Es la evidencia que permite entender un Acceptance Gate rojo sin
     * reproducir el fallo a mano.
     */
    @After("@web")
    public void cerrarNavegador(Scenario escenario) {
        if (navegador == null) {
            return;
        }
        try {
            if (escenario.isFailed()) {
                byte[] captura = ((TakesScreenshot) navegador).getScreenshotAs(OutputType.BYTES);
                escenario.attach(captura, "image/png", "Pantalla al momento del fallo");
            }
        } finally {
            navegador.quit();
        }
    }

    @Dado("que el cliente abre el Portal de Clientes en el navegador")
    public void abrirPortal() {
        paginaLogin.abrir();
    }

    @Cuando("ingresa con el usuario {string} y la contraseña {string}")
    public void ingresarCon(String usuario, String contrasena) {
        paginaLogin.ingresar(usuario, contrasena);
    }

    @Cuando("ingresa con un usuario no registrado")
    public void ingresarConUsuarioNoRegistrado() {
        paginaLogin.ingresar("visitante-" + UUID.randomUUID(), "Clave-Incorrecta-1");
    }

    @Cuando("ingresa con el usuario {string} sin escribir la contraseña")
    public void ingresarSinContrasena(String usuario) {
        paginaLogin.ingresar(usuario, "");
    }

    @Entonces("ve el mensaje de éxito {string}")
    public void verMensajeDeExito(String mensajeEsperado) {
        assertEquals(mensajeEsperado, paginaLogin.mensajeMostrado());
        assertEquals("exito", paginaLogin.estadoDelMensaje(), "El mensaje debe mostrarse como exito");
    }

    @Entonces("ve el mensaje de error {string}")
    public void verMensajeDeError(String mensajeEsperado) {
        assertEquals(mensajeEsperado, paginaLogin.mensajeMostrado());
        assertEquals("error", paginaLogin.estadoDelMensaje(), "El mensaje debe mostrarse como error");
    }

    @Entonces("el portal muestra la versión que se está desplegando")
    public void verVersionDesplegada() {
        String versionMostrada = paginaLogin.versionMostrada();
        AmbienteBajoPrueba.versionEsperada().ifPresentOrElse(
                versionEsperada -> assertEquals(versionEsperada, versionMostrada,
                        "El portal responde con otra version: el trafico no llega a la version desplegada"),
                () -> assertFalse(versionMostrada.isBlank(), "El portal no informa su version"));
    }
}
