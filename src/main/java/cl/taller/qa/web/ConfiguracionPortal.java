package cl.taller.qa.web;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuracion del portal, leida de variables de entorno.
 *
 * <p>La misma imagen Docker se despliega en todos los ambientes; lo unico que
 * cambia entre ellos es la configuracion que recibe al arrancar (principio
 * "build once, deploy many"). Por eso nada de lo que distingue a un ambiente
 * vive dentro del codigo compilado.</p>
 *
 * <p>Se construye a partir de un {@code Map} y no leyendo {@code System.getenv()}
 * directamente, para que las pruebas unitarias puedan entregar cualquier
 * combinacion de variables sin tocar el entorno real del proceso.</p>
 *
 * @param puerto            puerto TCP donde escucha el servidor
 * @param version           version de la aplicacion desplegada
 * @param commit            SHA del commit que origino el artefacto (trazabilidad)
 * @param color             color del despliegue Blue-Green que atiende la peticion
 * @param usuariosIniciales usuarios de demostracion que se registran al arrancar
 */
public record ConfiguracionPortal(
        int puerto,
        String version,
        String commit,
        String color,
        Map<String, String> usuariosIniciales) {

    public static final String VARIABLE_PUERTO = "PUERTO";
    public static final String VARIABLE_VERSION = "VERSION_APP";
    public static final String VARIABLE_COMMIT = "COMMIT_SHA";
    public static final String VARIABLE_COLOR = "COLOR_DESPLIEGUE";
    public static final String VARIABLE_USUARIOS = "USUARIOS_DEMO";

    static final int PUERTO_POR_DEFECTO = 8080;
    static final String VALOR_LOCAL = "local";

    /**
     * Usuarios de demostracion. Son los mismos que usa la prueba de carga k6 y
     * los escenarios de aceptacion: un ambiente de pruebas sin datos conocidos
     * no se puede verificar de forma automatica.
     */
    static final String USUARIOS_POR_DEFECTO = "wmelgar:Segura2026!,cliente1:Clave123!,cliente2:Clave123!";

    /** Copia defensiva: el record es inmutable aunque reciba un mapa mutable. */
    public ConfiguracionPortal {
        usuariosIniciales = Map.copyOf(usuariosIniciales);
    }

    /**
     * Construye la configuracion aplicando valores por defecto a lo que falte.
     *
     * @param variables variables de entorno disponibles
     * @return configuracion lista para iniciar el servidor
     * @throws IllegalArgumentException si alguna variable trae un valor invalido
     */
    public static ConfiguracionPortal desdeVariables(Map<String, String> variables) {
        return new ConfiguracionPortal(
                leerPuerto(variables.get(VARIABLE_PUERTO)),
                valorOPorDefecto(variables.get(VARIABLE_VERSION), versionDelArtefacto()),
                valorOPorDefecto(variables.get(VARIABLE_COMMIT), VALOR_LOCAL),
                valorOPorDefecto(variables.get(VARIABLE_COLOR), VALOR_LOCAL),
                leerUsuarios(valorOPorDefecto(variables.get(VARIABLE_USUARIOS), USUARIOS_POR_DEFECTO)));
    }

    private static int leerPuerto(String valor) {
        if (esVacio(valor)) {
            return PUERTO_POR_DEFECTO;
        }
        try {
            int puerto = Integer.parseInt(valor.trim());
            // 0 es valido: el sistema operativo asigna un puerto libre (lo usan las pruebas).
            if (puerto < 0 || puerto > 65_535) {
                throw new IllegalArgumentException(mensajePuertoInvalido(valor));
            }
            return puerto;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(mensajePuertoInvalido(valor), e);
        }
    }

    /**
     * Interpreta la lista de usuarios con formato {@code usuario:clave,usuario:clave}.
     * Solo se separa por el primer ':' para admitir claves que lo contengan.
     */
    private static Map<String, String> leerUsuarios(String valor) {
        Map<String, String> usuarios = new LinkedHashMap<>();
        for (String entrada : valor.split(",")) {
            String limpia = entrada.trim();
            int separador = limpia.indexOf(':');
            if (separador <= 0 || separador == limpia.length() - 1) {
                throw new IllegalArgumentException(
                        "La variable " + VARIABLE_USUARIOS + " debe tener el formato usuario:clave,usuario:clave");
            }
            usuarios.put(limpia.substring(0, separador), limpia.substring(separador + 1));
        }
        return usuarios;
    }

    /** Version declarada en el MANIFEST del jar; fuera de un jar (IDE, pruebas) no existe. */
    private static String versionDelArtefacto() {
        String version = ConfiguracionPortal.class.getPackage().getImplementationVersion();
        return esVacio(version) ? "desarrollo" : version;
    }

    private static String valorOPorDefecto(String valor, String porDefecto) {
        return esVacio(valor) ? porDefecto : valor.trim();
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static String mensajePuertoInvalido(String valor) {
        return "La variable " + VARIABLE_PUERTO + " debe ser un numero entre 0 y 65535, pero vale '" + valor + "'";
    }
}
