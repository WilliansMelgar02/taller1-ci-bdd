package cl.taller.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pruebas unitarias de la operacion DIVISION.
 *
 * <p>Al igual que la multiplicacion, este metodo quedaba sin cubrir. Su caso
 * mas importante no es el camino feliz sino el borde: dividir por cero. Una
 * suite que solo prueba lo que funciona da una falsa sensacion de seguridad,
 * porque los defectos viven casi siempre en los bordes.</p>
 */
@DisplayName("Calculadora - operacion division")
class CalculadoraDivisionTest {

    private Calculadora calculadora;

    @BeforeEach
    void prepararEscenario() {
        calculadora = new Calculadora();
    }

    @Test
    @DisplayName("Division exacta devuelve el cociente esperado")
    void dividirDivisionExacta() {
        // Arrange
        int dividendo = 20;
        int divisor = 4;

        // Act
        double resultado = calculadora.dividir(dividendo, divisor);

        // Assert
        assertEquals(5.0, resultado, 0.0001, "20 / 4 debe ser 5");
    }

    @Test
    @DisplayName("Division inexacta conserva la parte decimal")
    void dividirConservaDecimales() {
        double resultado = calculadora.dividir(7, 2);

        assertEquals(3.5, resultado, 0.0001, "7 / 2 debe ser 3,5 y no 3");
    }

    @Test
    @DisplayName("Dividir por cero lanza ArithmeticException con un mensaje claro")
    void dividirPorCeroLanzaExcepcion() {
        // El comportamiento esperado ante un error tambien es parte del contrato:
        // se verifica el tipo de excepcion Y el mensaje que recibe quien la atrapa.
        ArithmeticException excepcion = assertThrows(
                ArithmeticException.class,
                () -> calculadora.dividir(10, 0),
                "Dividir por cero debe lanzar ArithmeticException");

        assertEquals("No es posible dividir por cero", excepcion.getMessage(),
                "El mensaje debe explicar la causa sin exponer detalles internos");
    }

    @ParameterizedTest(name = "{0} / {1} = {2}")
    @CsvSource({
        "10, 2, 5.0",
        "9, 3, 3.0",
        "-12, 4, -3.0",
        "1, 4, 0.25"
    })
    @DisplayName("Division parametrizada con multiples juegos de datos")
    void dividirConDistintosDatos(int dividendo, int divisor, double esperado) {
        assertEquals(esperado, calculadora.dividir(dividendo, divisor), 0.0001);
    }
}
