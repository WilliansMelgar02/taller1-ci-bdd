package cl.taller.qa.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cl.taller.qa.EstadoAutenticacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pruebas unitarias de la traduccion de resultados de negocio a codigos HTTP.
 *
 * <p>Es la unica decision propia del manejador y se prueba sin levantar un
 * servidor: la prueba corre en milisegundos. El comportamiento HTTP completo
 * (cabeceras, cuerpo, rutas) lo cubren las pruebas de integracion.</p>
 */
@DisplayName("API de login - codigo HTTP segun el resultado de negocio")
class ManejadorLoginTest {

    @ParameterizedTest(name = "{0} responde HTTP {1}")
    @CsvSource({
        "CONCEDIDO, 200",
        "DATOS_INCOMPLETOS, 400",
        "CREDENCIALES_INVALIDAS, 401",
        "CUENTA_BLOQUEADA, 423"
    })
    @DisplayName("Cada desenlace de negocio tiene su codigo HTTP")
    void traduceCadaEstadoAUnCodigoHttp(EstadoAutenticacion estado, int codigoEsperado) {
        assertEquals(codigoEsperado, ManejadorLogin.codigoHttpPara(estado));
    }
}
