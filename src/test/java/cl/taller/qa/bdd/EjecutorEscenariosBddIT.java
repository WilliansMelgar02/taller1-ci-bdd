package cl.taller.qa.bdd;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Runner de los escenarios BDD.
 *
 * <p>Declara una suite de la JUnit Platform que delega la ejecucion en el motor
 * de Cucumber. El sufijo <b>IT</b> hace que la ejecute el plugin
 * <i>maven-failsafe-plugin</i> durante {@code mvn verify}, y no Surefire.
 * De este modo las pruebas unitarias (segundos) quedan separadas de las
 * pruebas de aceptacion, y el pipeline puede fallar temprano y barato.</p>
 *
 * <p>La configuracion de glue y de reportes vive en
 * {@code src/test/resources/junit-platform.properties} para no mezclar
 * responsabilidades dentro de la clase.</p>
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
public class EjecutorEscenariosBddIT {
    // Clase intencionalmente vacia: solo actua como punto de entrada declarativo.
}
