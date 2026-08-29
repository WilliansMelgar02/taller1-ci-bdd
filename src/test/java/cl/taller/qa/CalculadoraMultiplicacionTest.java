package cl.taller.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pruebas unitarias de la operacion MULTIPLICACION.
 *
 * <p>Esta clase nacio de un hallazgo del analisis de cobertura: el metodo
 * {@code multiplicar} era publico pero ninguna prueba lo ejercitaba, algo que
 * el umbral global de JaCoCo no detectaba porque el promedio del proyecto lo
 * compensaba con las clases bien cubiertas. La regla por clase lo dejo a la
 * vista.</p>
 *
 * <p>Mantiene los mismos criterios de atomicidad que el resto de la suite:
 * instancia nueva por prueba, sin dependencia de orden y una sola razon de
 * fallo por caso.</p>
 */
@DisplayName("Calculadora - operacion multiplicacion")
class CalculadoraMultiplicacionTest {

    private Calculadora calculadora;

    @BeforeEach
    void prepararEscenario() {
        calculadora = new Calculadora();
    }

    @Test
    @DisplayName("Multiplicacion de dos numeros positivos devuelve el producto esperado")
    void multiplicarDosNumerosPositivos() {
        // Arrange
        int factorA = 6;
        int factorB = 7;

        // Act
        int resultado = calculadora.multiplicar(factorA, factorB);

        // Assert
        assertEquals(42, resultado, "6 x 7 debe ser 42");
    }

    @Test
    @DisplayName("Multiplicar por cero siempre devuelve cero")
    void multiplicarPorCero() {
        int resultado = calculadora.multiplicar(1234, 0);

        assertEquals(0, resultado, "Todo numero multiplicado por cero es cero");
    }

    @Test
    @DisplayName("El uno es el elemento neutro de la multiplicacion")
    void multiplicarPorElementoNeutro() {
        int resultado = calculadora.multiplicar(-15, 1);

        assertEquals(-15, resultado, "Multiplicar por uno no debe alterar el operando");
    }

    @ParameterizedTest(name = "{0} x {1} = {2}")
    @CsvSource({
        "3, 4, 12",
        "-5, 6, -30",
        "-7, -8, 56",
        "10000, 100, 1000000"
    })
    @DisplayName("Multiplicacion parametrizada con multiples juegos de datos")
    void multiplicarConDistintosDatos(int factorA, int factorB, int esperado) {
        assertEquals(esperado, calculadora.multiplicar(factorA, factorB));
    }
}
