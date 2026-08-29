package cl.taller.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pruebas unitarias de la operacion SUMA.
 *
 * <p>Criterios de atomicidad aplicados:</p>
 * <ul>
 *   <li><b>Independencia:</b> {@code @BeforeEach} construye una instancia nueva
 *       para cada prueba, por lo que no se comparte estado entre casos.</li>
 *   <li><b>Sin orden implicito:</b> las pruebas pueden ejecutarse en cualquier
 *       secuencia, incluso en paralelo, y el resultado no cambia.</li>
 *   <li><b>Una sola razon de fallo:</b> cada prueba verifica un unico
 *       comportamiento, de modo que un fallo apunta a una causa concreta.</li>
 *   <li><b>Alta cohesion:</b> la clase agrupa solo pruebas de la suma.</li>
 * </ul>
 */
@DisplayName("Calculadora - operacion suma")
class CalculadoraSumaTest {

    private Calculadora calculadora;

    @BeforeEach
    void prepararEscenario() {
        // Arrange comun: instancia limpia por cada prueba (aislamiento total).
        calculadora = new Calculadora();
    }

    @Test
    @DisplayName("Suma de dos numeros positivos devuelve el total esperado")
    void sumarDosNumerosPositivos() {
        // Arrange
        int sumandoA = 7;
        int sumandoB = 5;

        // Act
        int resultado = calculadora.sumar(sumandoA, sumandoB);

        // Assert
        assertEquals(12, resultado, "7 + 5 debe ser 12");
    }

    @Test
    @DisplayName("Suma con numeros negativos respeta el signo")
    void sumarNumerosNegativos() {
        int resultado = calculadora.sumar(-8, 3);

        assertEquals(-5, resultado, "-8 + 3 debe ser -5");
    }

    @Test
    @DisplayName("El cero es el elemento neutro de la suma")
    void sumarConElementoNeutro() {
        int resultado = calculadora.sumar(42, 0);

        assertEquals(42, resultado, "Sumar cero no debe alterar el operando");
    }

    /**
     * Prueba parametrizada: un mismo comportamiento verificado con varios
     * juegos de datos. Cada fila es un caso independiente en el reporte.
     */
    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({
        "1, 1, 2",
        "100, 250, 350",
        "-10, -15, -25",
        "2147483646, 1, 2147483647"
    })
    @DisplayName("Suma parametrizada con multiples juegos de datos")
    void sumarConDistintosDatos(int sumandoA, int sumandoB, int esperado) {
        assertEquals(esperado, calculadora.sumar(sumandoA, sumandoB));
    }
}
