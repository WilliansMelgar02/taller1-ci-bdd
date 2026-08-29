package cl.gespron.taller;

/**
 * Servicio de operaciones aritmeticas basicas.
 *
 * <p>Clase deliberadamente sin estado (stateless): cada operacion depende
 * unicamente de sus parametros de entrada. Esto es lo que permite escribir
 * pruebas <b>atomicas</b>: ninguna prueba puede contaminar a otra porque no
 * existe informacion compartida entre invocaciones.</p>
 *
 * <p>Alta cohesion: la clase hace una sola cosa (calcular).
 * Bajo acoplamiento: no depende de ninguna otra clase del proyecto.</p>
 */
public class Calculadora {

    /**
     * Suma dos numeros enteros.
     *
     * @param sumandoA primer operando
     * @param sumandoB segundo operando
     * @return la suma de ambos operandos
     */
    public int sumar(int sumandoA, int sumandoB) {
        return sumandoA + sumandoB;
    }

    /**
     * Resta el sustraendo al minuendo.
     *
     * @param minuendo   numero del cual se resta
     * @param sustraendo numero que se resta
     * @return la diferencia entre ambos operandos
     */
    public int restar(int minuendo, int sustraendo) {
        return minuendo - sustraendo;
    }

    /**
     * Multiplica dos numeros enteros.
     *
     * @param factorA primer factor
     * @param factorB segundo factor
     * @return el producto de ambos factores
     */
    public int multiplicar(int factorA, int factorB) {
        return factorA * factorB;
    }

    /**
     * Divide el dividendo por el divisor.
     *
     * @param dividendo numero a dividir
     * @param divisor   numero por el cual se divide
     * @return el cociente con precision decimal
     * @throws ArithmeticException si el divisor es cero
     */
    public double dividir(int dividendo, int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("No es posible dividir por cero");
        }
        return (double) dividendo / divisor;
    }
}
