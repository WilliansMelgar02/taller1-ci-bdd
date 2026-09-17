package cl.taller.qa.aceptacion.soporte;

import java.time.Duration;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object de la pagina de ingreso del portal.
 *
 * <p>Concentra en un solo lugar los selectores y la forma de interactuar con
 * la pagina. Los pasos de Cucumber hablan en lenguaje de negocio
 * ("ingresa con...") y nunca tocan un selector: si manana cambia el HTML, se
 * corrige esta clase y ningun escenario.</p>
 *
 * <p>No hay pausas fijas ({@code Thread.sleep}): cada lectura espera
 * explicitamente la condicion que necesita, con un limite de tiempo. Es mas
 * rapido cuando la pagina responde bien y evita pruebas intermitentes.</p>
 */
public class PaginaLogin {

    private static final By CAMPO_USUARIO = By.id("usuario");
    private static final By CAMPO_CONTRASENA = By.id("contrasena");
    private static final By BOTON_INGRESAR = By.id("boton-ingresar");
    private static final By MENSAJE = By.id("mensaje");
    private static final By PIE_VERSION = By.id("version-desplegada");

    /** Estados del mensaje que indican que la respuesta del servidor ya llego. */
    private static final Set<String> ESTADOS_FINALES = Set.of("exito", "error");

    private final WebDriver navegador;
    private final WebDriverWait espera;

    public PaginaLogin(WebDriver navegador) {
        this.navegador = navegador;
        this.espera = new WebDriverWait(navegador, Duration.ofSeconds(10));
    }

    /** Navega a la pagina y espera a que el formulario este disponible. */
    public void abrir() {
        navegador.get(AmbienteBajoPrueba.urlBase() + "/");
        espera.until(ExpectedConditions.elementToBeClickable(BOTON_INGRESAR));
    }

    /**
     * Completa el formulario y lo envia.
     *
     * @param usuario    texto a escribir en el campo usuario
     * @param contrasena texto a escribir en el campo contrasena
     */
    public void ingresar(String usuario, String contrasena) {
        escribir(CAMPO_USUARIO, usuario);
        escribir(CAMPO_CONTRASENA, contrasena);
        navegador.findElement(BOTON_INGRESAR).click();
    }

    /**
     * Espera la respuesta del servidor y devuelve el mensaje mostrado.
     *
     * @return texto visible del mensaje
     */
    public String mensajeMostrado() {
        esperarRespuesta();
        return navegador.findElement(MENSAJE).getText();
    }

    /**
     * Estado visual del mensaje ({@code exito} o {@code error}).
     *
     * @return valor del atributo data-estado
     */
    public String estadoDelMensaje() {
        esperarRespuesta();
        return navegador.findElement(MENSAJE).getDomAttribute("data-estado");
    }

    /**
     * Version que la pagina informa en su pie, una vez consultado /health.
     *
     * @return version desplegada segun la propia pagina
     */
    public String versionMostrada() {
        espera.until(ExpectedConditions.attributeToBeNotEmpty(navegador.findElement(PIE_VERSION), "data-version"));
        return navegador.findElement(PIE_VERSION).getDomAttribute("data-version");
    }

    private void esperarRespuesta() {
        espera.until(driver -> ESTADOS_FINALES.contains(
                driver.findElement(MENSAJE).getDomAttribute("data-estado")));
    }

    private void escribir(By campo, String texto) {
        WebElement elemento = navegador.findElement(campo);
        elemento.clear();
        elemento.sendKeys(texto);
    }
}
