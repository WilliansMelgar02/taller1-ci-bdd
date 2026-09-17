package cl.taller.qa.aceptacion;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Runner de las pruebas de ACEPTACION (Acceptance Gate del deployment pipeline).
 *
 * <p>A diferencia de los escenarios BDD de {@code features/}, que validan las
 * reglas de negocio contra el servicio en memoria, estas pruebas se ejecutan
 * contra la aplicacion <b>ya desplegada</b> en el ambiente de pruebas: pasan por
 * el navegador, la red, el contenedor Docker y el proxy. Por eso no corren en
 * {@code mvn verify}, sino solo con el perfil {@code aceptacion}:</p>
 *
 * <pre>
 * mvn verify -Paceptacion -Durl.base=http://localhost:8082 -Dversion.esperada=1.1.0-a1b2c3d
 * </pre>
 *
 * <p>La configuracion va en la propia clase, y no en
 * {@code junit-platform.properties}, porque ese archivo pertenece a la suite BDD:
 * cada suite declara su glue y sus reportes sin pisar los de la otra.</p>
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("aceptacion")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "cl.taller.qa.aceptacion.pasos")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@aceptacion")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, "
        + "html:target/aceptacion-reports/reporte-aceptacion.html, "
        + "json:target/aceptacion-reports/aceptacion.json, "
        + "junit:target/aceptacion-reports/aceptacion-junit.xml")
public class EjecutorAceptacionIT {
    // Clase intencionalmente vacia: solo actua como punto de entrada declarativo.
}
