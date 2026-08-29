package cl.gespron.taller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pruebas unitarias de la operacion RESTA.
 *
 * <p>Esta clase es totalmente independiente de {@link CalculadoraSumaTest}:
 * no comparte instancias, ni archivos, ni orden de ejecucion. Eliminar o
 * ejecutar aisladamente cualquiera de las dos clases no afecta a la otra,
 * que es la definicion practica de <b>bajo acoplamiento</b> entre pruebas.</p>
 */
@DisplayName("Calculadora - operacion resta")
class CalculadoraRestaTest {

    private Calculadora calculadora;

    @BeforeEach
    void prepararEscenario() {
        calculadora = new Calculadora();
    }

    @Test
    @DisplayName("Resta de dos numeros positivos devuelve la diferencia esperada")
    void restarDosNumerosPositivos() {
        // Arrange
        int minuendo = 20;
        int sustraendo = 8;

        // Act
        int resultado = calculadora.restar(minuendo, sustraendo);

        // Assert
        assertEquals(12, resultado, "20 - 8 debe ser 12");
    }

    @Test
    @DisplayName("Restar un numero mayor produce un resultado negativo")
    void restarProduceResultadoNegativo() {
        int resultado = calculadora.restar(5, 9);

        assertTrue(resultado < 0, "El resultado debe ser negativo");
        assertEquals(-4, resultado, "5 - 9 debe ser -4");
    }

    @Test
    @DisplayName("Restar un numero a si mismo devuelve cero")
    void restarNumeroASiMismo() {
        int resultado = calculadora.restar(33, 33);

        assertEquals(0, resultado, "Todo numero menos si mismo es cero");
    }

    @ParameterizedTest(name = "{0} - {1} = {2}")
    @CsvSource({
        "10, 4, 6",
        "0, 7, -7",
        "-5, -5, 0",
        "1000, 1, 999"
    })
    @DisplayName("Resta parametrizada con multiples juegos de datos")
    void restarConDistintosDatos(int minuendo, int sustraendo, int esperado) {
        assertEquals(esperado, calculadora.restar(minuendo, sustraendo));
    }
}
