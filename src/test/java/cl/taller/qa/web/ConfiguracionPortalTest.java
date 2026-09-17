package cl.taller.qa.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas unitarias de la lectura de configuracion del portal.
 *
 * <p>La configuracion decide en que puerto escucha el contenedor y que
 * version informa al pipeline. Un error aqui no lo detecta ninguna prueba de
 * negocio, pero rompe el despliegue: por eso se prueban tanto los valores por
 * defecto como los valores invalidos, que deben fallar con un mensaje que
 * permita corregir la variable sin leer el codigo.</p>
 */
@DisplayName("Configuracion del portal desde variables de entorno")
class ConfiguracionPortalTest {

    @Test
    @DisplayName("Sin variables se usan los valores por defecto")
    void sinVariablesUsaValoresPorDefecto() {
        ConfiguracionPortal configuracion = ConfiguracionPortal.desdeVariables(Map.of());

        assertEquals(8080, configuracion.puerto(), "El puerto por defecto debe ser 8080");
        assertEquals("local", configuracion.commit());
        assertEquals("local", configuracion.color());
        assertEquals("desarrollo", configuracion.version(),
                "Fuera de un jar no hay MANIFEST, por lo que la version debe indicarlo");
        assertEquals("Segura2026!", configuracion.usuariosIniciales().get("wmelgar"),
                "Los usuarios de demostracion deben coincidir con los de la prueba de carga");
    }

    @Test
    @DisplayName("Las variables de entorno reemplazan a los valores por defecto")
    void leeLasVariablesDeEntorno() {
        Map<String, String> variables = Map.of(
                "PUERTO", "9090",
                "VERSION_APP", "1.1.0",
                "COMMIT_SHA", "a1b2c3d",
                "COLOR_DESPLIEGUE", "green",
                "USUARIOS_DEMO", "ana:clave1, luis:clave:con:dos-puntos");

        ConfiguracionPortal configuracion = ConfiguracionPortal.desdeVariables(variables);

        assertEquals(9090, configuracion.puerto());
        assertEquals("1.1.0", configuracion.version());
        assertEquals("a1b2c3d", configuracion.commit());
        assertEquals("green", configuracion.color());
        assertEquals(Map.of("ana", "clave1", "luis", "clave:con:dos-puntos"), configuracion.usuariosIniciales(),
                "Solo el primer ':' separa usuario de clave");
    }

    @Test
    @DisplayName("Una variable vacia equivale a no definirla")
    void variableVaciaUsaValorPorDefecto() {
        ConfiguracionPortal configuracion = ConfiguracionPortal.desdeVariables(Map.of("PUERTO", "  "));

        assertEquals(8080, configuracion.puerto());
    }

    @ParameterizedTest(name = "PUERTO=''{0}'' es rechazado")
    @ValueSource(strings = {"ocho-mil", "-1", "65536", "80.5"})
    @DisplayName("Un puerto invalido detiene el arranque con un mensaje claro")
    void puertoInvalidoLanzaExcepcion(String puerto) {
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> ConfiguracionPortal.desdeVariables(Map.of("PUERTO", puerto)));

        assertTrue(excepcion.getMessage().contains("PUERTO"),
                "El mensaje debe nombrar la variable que hay que corregir");
    }

    @ParameterizedTest(name = "USUARIOS_DEMO=''{0}'' es rechazado")
    @ValueSource(strings = {"sin-separador", ":clave", "usuario:", "ana:clave1,,luis:clave2"})
    @DisplayName("Una lista de usuarios mal formada detiene el arranque")
    void usuariosMalFormadosLanzaExcepcion(String usuarios) {
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> ConfiguracionPortal.desdeVariables(Map.of("USUARIOS_DEMO", usuarios)));

        assertTrue(excepcion.getMessage().contains("usuario:clave"),
                "El mensaje debe mostrar el formato esperado");
    }

    @Test
    @DisplayName("La configuracion no cambia si se modifica el mapa original")
    void usuariosSonInmutables() {
        Map<String, String> usuarios = new HashMap<>(Map.of("ana", "clave1"));
        ConfiguracionPortal configuracion = new ConfiguracionPortal(8080, "1.0", "abc", "blue", usuarios);

        usuarios.put("intruso", "clave");

        assertEquals(1, configuracion.usuariosIniciales().size(),
                "El record debe guardar una copia defensiva de los usuarios");
        assertThrows(UnsupportedOperationException.class,
                () -> configuracion.usuariosIniciales().put("otro", "clave"));
    }
}
