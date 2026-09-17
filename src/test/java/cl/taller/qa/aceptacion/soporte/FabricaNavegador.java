package cl.taller.qa.aceptacion.soporte;

import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Crea el navegador que usan las pruebas de aceptacion.
 *
 * <p>Selenium Manager (incluido en Selenium 4) descarga el ChromeDriver que
 * corresponde a la version de Chrome instalada, por lo que no hay binarios
 * versionados en el repositorio ni rutas distintas entre Windows y el agente
 * Linux del pipeline.</p>
 */
public final class FabricaNavegador {

    private FabricaNavegador() {
        // Solo expone metodos estaticos.
    }

    /**
     * Abre un navegador Chrome nuevo y aislado (perfil temporal propio).
     *
     * @return navegador listo para usar; quien lo pide debe cerrarlo
     */
    public static WebDriver abrirChrome() {
        ChromeOptions opciones = new ChromeOptions();
        if (!AmbienteBajoPrueba.navegadorVisible()) {
            // Sin interfaz grafica: el agente de CI no tiene pantalla.
            opciones.addArguments("--headless=new");
        }
        // Tamano fijo: la misma resolucion en local y en CI evita diferencias de diseno.
        opciones.addArguments("--window-size=1280,900");
        // Necesarios dentro de contenedores y agentes Linux con poca memoria compartida.
        opciones.addArguments("--no-sandbox", "--disable-dev-shm-usage");

        WebDriver navegador = new ChromeDriver(opciones);
        navegador.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        return navegador;
    }
}
