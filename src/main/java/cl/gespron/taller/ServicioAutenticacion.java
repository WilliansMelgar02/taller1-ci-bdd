package cl.gespron.taller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Servicio de inicio de sesion del Portal de Clientes.
 *
 * <p>Implementa las reglas de negocio acordadas en la sesion Three Amigos
 * documentada en {@code docs/01-sesion-three-amigos.md}:</p>
 *
 * <ul>
 *   <li><b>RN-01</b> acceso solo con usuario y clave coincidentes.</li>
 *   <li><b>RN-02</b> la clave distingue mayusculas de minusculas.</li>
 *   <li><b>RN-03</b> el mensaje de error es generico, no revela que dato fallo.</li>
 *   <li><b>RN-04</b> la cuenta se bloquea tras 3 intentos fallidos consecutivos.</li>
 *   <li><b>RN-05</b> un ingreso exitoso reinicia el contador de intentos.</li>
 *   <li><b>RN-06</b> con campos vacios se piden los datos y no se descuenta intento.</li>
 * </ul>
 *
 * <p>El repositorio de usuarios es en memoria a proposito: mantiene la prueba
 * rapida y determinista, sin depender de una base de datos externa.</p>
 */
public class ServicioAutenticacion {

    /** Cantidad de intentos fallidos consecutivos que provocan el bloqueo (RN-04). */
    public static final int MAX_INTENTOS_PERMITIDOS = 3;

    public static final String MSG_CREDENCIALES_INVALIDAS = "Credenciales inválidas";
    public static final String MSG_CUENTA_BLOQUEADA = "Cuenta bloqueada por múltiples intentos fallidos";
    public static final String MSG_DATOS_INCOMPLETOS = "Debe ingresar usuario y contraseña";

    private final Map<String, String> usuariosRegistrados = new HashMap<>();
    private final Map<String, Integer> intentosFallidos = new HashMap<>();
    private final Set<String> cuentasBloqueadas = new HashSet<>();

    /**
     * Registra un usuario habilitado para ingresar al portal.
     *
     * @param usuario identificador del cliente
     * @param clave   contrasena en texto plano (solo para efectos del taller)
     */
    public void registrarUsuario(String usuario, String clave) {
        usuariosRegistrados.put(usuario, clave);
    }

    /**
     * Valida las credenciales entregadas y aplica la politica de bloqueo.
     *
     * @param usuario identificador ingresado
     * @param clave   contrasena ingresada
     * @return el resultado del intento, nunca {@code null}
     */
    public ResultadoAutenticacion autenticar(String usuario, String clave) {
        // RN-06: datos incompletos no consumen intentos.
        if (esVacio(usuario) || esVacio(clave)) {
            return new ResultadoAutenticacion(
                    false, MSG_DATOS_INCOMPLETOS, intentosRestantes(usuario), false);
        }

        // RN-04: una cuenta bloqueada no vuelve a validar credenciales.
        if (cuentasBloqueadas.contains(usuario)) {
            return new ResultadoAutenticacion(false, MSG_CUENTA_BLOQUEADA, 0, true);
        }

        // RN-01 y RN-02: comparacion exacta, sensible a mayusculas.
        String claveEsperada = usuariosRegistrados.get(usuario);
        boolean credencialesValidas = claveEsperada != null && claveEsperada.equals(clave);

        if (credencialesValidas) {
            // RN-05: el acierto reinicia el contador.
            intentosFallidos.remove(usuario);
            return new ResultadoAutenticacion(
                    true, "Bienvenido/a, " + usuario, MAX_INTENTOS_PERMITIDOS, false);
        }

        return registrarIntentoFallido(usuario);
    }

    /**
     * Incrementa el contador del usuario y bloquea la cuenta si corresponde.
     *
     * @param usuario identificador ingresado
     * @return resultado denegado, con o sin bloqueo segun el contador
     */
    private ResultadoAutenticacion registrarIntentoFallido(String usuario) {
        int fallosAcumulados = intentosFallidos.getOrDefault(usuario, 0) + 1;
        intentosFallidos.put(usuario, fallosAcumulados);

        if (fallosAcumulados >= MAX_INTENTOS_PERMITIDOS) {
            cuentasBloqueadas.add(usuario);
            return new ResultadoAutenticacion(false, MSG_CUENTA_BLOQUEADA, 0, true);
        }

        // RN-03: mensaje generico, no revela si fallo el usuario o la clave.
        return new ResultadoAutenticacion(
                false,
                MSG_CREDENCIALES_INVALIDAS,
                MAX_INTENTOS_PERMITIDOS - fallosAcumulados,
                false);
    }

    /**
     * Intentos que le quedan al usuario antes del bloqueo.
     *
     * @param usuario identificador consultado
     * @return intentos disponibles
     */
    public int intentosRestantes(String usuario) {
        if (cuentasBloqueadas.contains(usuario)) {
            return 0;
        }
        return MAX_INTENTOS_PERMITIDOS - intentosFallidos.getOrDefault(usuario, 0);
    }

    /**
     * Indica si la cuenta esta bloqueada.
     *
     * @param usuario identificador consultado
     * @return true si la cuenta fue bloqueada
     */
    public boolean estaBloqueada(String usuario) {
        return cuentasBloqueadas.contains(usuario);
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
