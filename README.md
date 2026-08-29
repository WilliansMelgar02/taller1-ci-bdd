# Taller 1 · Evaluación Unidad N°II — Integración Continua, BDD y Performance

[![CI - Pruebas Automatizadas](https://github.com/WilliansMelgar02/taller1-ci-bdd/actions/workflows/ci.yml/badge.svg)](https://github.com/WilliansMelgar02/taller1-ci-bdd/actions/workflows/ci.yml)

**Autor:** Willians Eduardo Melgar Cherres
**Asignatura:** Automatización de Pruebas — Unidad II
**Repositorio:** <https://github.com/WilliansMelgar02/taller1-ci-bdd>

---

## Índice

1. [Objetivo](#1-objetivo)
2. [Stack tecnológico y por qué](#2-stack-tecnológico-y-por-qué)
3. [Estructura del proyecto](#3-estructura-del-proyecto)
4. [Puesta en marcha y comandos](#4-puesta-en-marcha-y-comandos)
5. [Actividad 1 — Integración continua](#5-actividad-1--integración-continua)
6. [Actividad 2 — BDD, performance y observabilidad](#6-actividad-2--bdd-performance-y-observabilidad)
7. [El pipeline explicado etapa por etapa](#7-el-pipeline-explicado-etapa-por-etapa)
8. [Reportes navegables](#8-reportes-navegables)
9. [Resultados obtenidos](#9-resultados-obtenidos)
10. [Evidencias](#10-evidencias)

---

## 1. Objetivo

Profesionalizar el proceso de pruebas automatizadas de un proyecto Java
demostrando, sobre un caso real y ejecutable:

- gestión de versiones con **Git** (ramas, commits atómicos, merges revisados);
- gestión de dependencias y ciclo de build con **Maven**;
- pruebas unitarias **atómicas e independientes** con **JUnit 5**;
- especificación por ejemplos (**BDD**) con **Gherkin + Cucumber**, nacida de una
  sesión **Three Amigos**;
- un **pipeline de CI** que compila, prueba y publica reportes en cada `push` y
  cada *Pull Request*;
- una **prueba de performance** con **k6** cuyos umbrales actúan como *quality gate*;
- **métricas, dashboard y alertas automáticas** que hacen visible la calidad.

El proyecto no es un ejemplo de papel: **todo lo que se documenta aquí se ejecutó
realmente**, y las cifras que aparecen provienen de esas ejecuciones.

---

## 2. Stack tecnológico y por qué

| Herramienta | Versión | Por qué se eligió |
|---|---|---|
| **Java** | 17 (LTS) | Versión con soporte extendido; `record` simplifica los objetos de valor |
| **Maven** | 3.9.9 | Estándar del ecosistema Java; ciclo de vida claro y dependencias declarativas |
| **JUnit 5 (Jupiter)** | 5.11.4 | `@DisplayName` documenta la intención; `@ParameterizedTest` evita duplicar pruebas |
| **Cucumber** | 7.20.1 | Traduce los ejemplos de la sesión Three Amigos a pruebas ejecutables sin perder el lenguaje de negocio |
| **Surefire / Failsafe** | 3.5.2 | Separan pruebas unitarias de pruebas de aceptación: distinto costo, distinta etapa |
| **k6** | 0.55.0 | Prueba de carga *como código*, versionable y con umbrales que rompen el build |
| **GitHub Actions** | — | Pipeline as Code, integrado al repositorio y a los Pull Requests |
| **Jenkins** | (declarativo) | Se incluye el `Jenkinsfile` equivalente para demostrar que la estrategia no depende de la herramienta |

> **Decisión clave: Surefire y Failsafe separados.** Las pruebas unitarias corren
> en `mvn test` (décimas de segundo) y las de aceptación en `mvn verify`. Así el
> pipeline **falla temprano y barato**: un error trivial no gasta minutos
> ejecutando escenarios de negocio.

---

## 3. Estructura del proyecto

```
taller1-ci-bdd/
├── .github/
│   └── workflows/
│       └── ci.yml                      Pipeline de CI (5 etapas)
├── docs/
│   ├── 01-sesion-three-amigos.md       Roles, reglas de negocio, criterios y ejemplos
│   ├── 02-dashboard-metricas.md        Qué se mide y cómo llega al dashboard
│   ├── 03-alertas-automaticas.md       Matriz de alertas, canales y escalamiento
│   └── evidencias/                     Capturas y salidas de consola reales
├── performance/
│   ├── servidor-mock.js                Servicio de login bajo prueba (contrato real)
│   ├── login-carga.js                  Prueba de carga k6 con umbrales de SLA
│   └── resultados/                     Reportes generados (no versionado)
├── reportes/
│   └── index.html                      Dashboard de calidad publicado en GitHub Pages
├── src/
│   ├── main/java/cl/taller/qa/
│   │   ├── Calculadora.java            Servicio aritmético sin estado
│   │   ├── ServicioAutenticacion.java  Reglas de negocio del login (RN-01 a RN-06)
│   │   └── ResultadoAutenticacion.java Objeto de valor inmutable
│   └── test/
│       ├── java/cl/taller/qa/
│       │   ├── CalculadoraSumaTest.java    Pruebas unitarias de la suma
│       │   ├── CalculadoraRestaTest.java   Pruebas unitarias de la resta
│       │   └── bdd/
│       │       ├── EjecutorEscenariosBddIT.java  Runner de Cucumber
│       │       └── steps/PasosLogin.java         Step definitions
│       └── resources/
│           ├── features/login.feature            Escenarios en Gherkin (español)
│           └── junit-platform.properties         Glue y reportes de Cucumber
├── .gitignore
├── Jenkinsfile                          Pipeline equivalente on-premise
├── pom.xml                              Dependencias, plugins y reporting
└── README.md
```

### Por qué esta estructura

- **`src/main` y `src/test` separados** — es la convención de Maven; el código de
  prueba nunca viaja al artefacto de producción.
- **El paquete `bdd` aislado** — permite que Surefire lo excluya con una sola
  regla y que Failsafe lo ejecute en otra etapa.
- **`docs/` versionado junto al código** — la documentación evoluciona con el
  proyecto y se revisa en el mismo Pull Request. Documentación que vive en otro
  lado, muere en otro lado.
- **`performance/` fuera de `src`** — no es código Java, no se compila; es un
  activo de prueba independiente del lenguaje de la aplicación.

---

## 4. Puesta en marcha y comandos

### Requisitos

- JDK 17 · Maven 3.9+ · Node.js 18+ (para el servidor bajo prueba) · k6 0.5+ (opcional, para la prueba de carga)

### Comandos usados en el proyecto

```bash
# --- Compilación y pruebas ------------------------------------------------
mvn clean compile                    # compila el código fuente
mvn test                             # SOLO pruebas unitarias (rápido, fail-fast)
mvn verify                           # unitarias + escenarios BDD
mvn verify -DskipUnitTests=true      # solo BDD (lo que hace el pipeline en la etapa 2)
mvn site                             # genera los reportes HTML navegables

# --- Prueba de performance ------------------------------------------------
node performance/servidor-mock.js &  # levanta el servicio bajo prueba (puerto 8088)
k6 run performance/login-carga.js    # ejecuta la carga y evalúa los umbrales

# --- Gestión de versiones -------------------------------------------------
git checkout -b feature/nombre       # rama por funcionalidad
git commit -m "tipo(alcance): ..."   # commits atómicos con Conventional Commits
git merge --no-ff feature/nombre     # merge que preserva la historia de la rama
git log --graph --oneline --all      # visualiza el árbol de ramas
```

### Archivos clave

| Archivo | Qué resuelve |
|---|---|
| `pom.xml` | Dependencias, separación Surefire/Failsafe, reporting HTML |
| `.gitignore` | Excluye artefactos regenerables, reportes, configuración de IDE y secretos |
| `.github/workflows/ci.yml` | Las 5 etapas del pipeline |
| `Jenkinsfile` | El mismo pipeline en Jenkins declarativo |
| `src/test/resources/features/login.feature` | La especificación ejecutable del negocio |
| `src/test/resources/junit-platform.properties` | Glue de Cucumber y formatos de reporte |
| `performance/login-carga.js` | Perfil de carga y umbrales del SLA |
| `reportes/index.html` | Dashboard de calidad |

---

## 5. Actividad 1 — Integración continua

### 5.1 Repositorio Git: ramas y commits

Se usó un flujo **feature branch + merge sin fast-forward**, que es el que
reproduce un trabajo en equipo con Pull Requests:

```
main ──●────────●────────●────────●────────●────────●
        \      / \      / \      / \      / \      /
         ●────●   ●●●●─●   ●────●   ●●───●   ●────●
      Maven      BDD login   CI    k6 perf  dashboard
```

| Rama | Propósito | Commits |
|---|---|---|
| `feature/configuracion-maven` | pom.xml, dependencias, plugins | 1 |
| `feature/pruebas-unitarias` | Calculadora + suites de suma y resta | 3 |
| `feature/bdd-login` | Three Amigos, servicio, feature, steps | 4 |
| `feature/pipeline-ci` | ci.yml + Jenkinsfile | 1 |
| `feature/performance-k6` | servidor mock, prueba k6, refactor de paquete | 2 |
| `feature/observabilidad` | dashboard, métricas, alertas | 1 |
| `docs/readme` | documentación final | 1 |

**Convención de mensajes: Conventional Commits.**

```
tipo(alcance): resumen en imperativo, máximo 72 caracteres

Cuerpo que explica el PORQUÉ del cambio, no el qué (el qué ya está en
el diff). Incluye el resultado de las pruebas cuando corresponde.
```

Tipos usados: `feat`, `test`, `docs`, `ci`, `chore`, `refactor`, `merge`.

> **Por qué `--no-ff` en los merges.** Un merge *fast-forward* aplasta la historia
> de la rama y hace imposible saber qué commits formaron parte de qué entrega.
> Con `--no-ff` queda un nodo de merge que documenta la integración, equivalente
> a la traza que deja un Pull Request aprobado.

### 5.2 Proyecto Maven y dependencias

Las versiones están centralizadas en `<properties>` y las familias de artefactos
se importan mediante **BOM** (`junit-bom`, `cucumber-bom`), de modo que las
dependencias individuales no declaran versión y no pueden desalinearse:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.junit</groupId><artifactId>junit-bom</artifactId>
      <version>${junit.version}</version><type>pom</type><scope>import</scope>
    </dependency>
    <dependency>
      <groupId>io.cucumber</groupId><artifactId>cucumber-bom</artifactId>
      <version>${cucumber.version}</version><type>pom</type><scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 5.3 Pruebas unitarias atómicas

Dos suites independientes: `CalculadoraSumaTest` y `CalculadoraRestaTest`,
**14 casos en total**.

```java
@DisplayName("Calculadora - operacion suma")
class CalculadoraSumaTest {

    private Calculadora calculadora;

    @BeforeEach
    void prepararEscenario() {
        // Instancia nueva por prueba: aislamiento total, sin estado compartido
        calculadora = new Calculadora();
    }

    @Test
    @DisplayName("Suma de dos numeros positivos devuelve el total esperado")
    void sumarDosNumerosPositivos() {
        int resultado = calculadora.sumar(7, 5);          // Act
        assertEquals(12, resultado, "7 + 5 debe ser 12"); // Assert
    }

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({ "1, 1, 2", "100, 250, 350", "-10, -15, -25", "2147483646, 1, 2147483647" })
    void sumarConDistintosDatos(int a, int b, int esperado) {
        assertEquals(esperado, calculadora.sumar(a, b));
    }
}
```

**Cómo se garantiza la atomicidad**

| Criterio | Implementación |
|---|---|
| Sin estado compartido | `@BeforeEach` crea una instancia nueva por prueba |
| Sin dependencia de orden | Ninguna prueba consume el resultado de otra; pueden correr en cualquier orden o en paralelo |
| Una sola razón de fallo | Cada prueba verifica un comportamiento; un fallo apunta a una causa concreta |
| **Alta cohesión** | Cada clase agrupa pruebas de una única operación |
| **Bajo acoplamiento** | Las suites no se conocen entre sí; `Calculadora` no depende de ninguna otra clase |
| Sin recursos externos | Sin base de datos, red ni archivos: ejecución determinista |

### 5.4 `.gitignore`

Excluye lo regenerable (`target/`), los reportes (`cucumber-reports/`,
`allure-results/`), la configuración personal de IDE (`.idea/`, `.vscode/`),
los archivos del sistema operativo y, sobre todo, los **secretos** (`.env`,
`credenciales.properties`). Se versiona el *script* del Maven wrapper pero no su
binario descargado.

### 5.5 Pipeline de CI

Ver la sección [7. El pipeline explicado etapa por etapa](#7-el-pipeline-explicado-etapa-por-etapa).

---

## 6. Actividad 2 — BDD, performance y observabilidad

### 6.1 Sesión Three Amigos

Documentada completa en [`docs/01-sesion-three-amigos.md`](docs/01-sesion-three-amigos.md).

| Rol | Pregunta que representa | Aporte |
|---|---|---|
| **Negocio** (Carolina Rojas) | ¿Qué problema resolvemos? | Valor y política de 3 intentos |
| **Desarrollo** (Willians Melgar) | ¿Cómo lo construimos? | Contador por usuario, no por sesión |
| **QA** (Daniela Fuentes) | ¿Qué puede salir mal? | Casos de borde: usuario inexistente, campos vacíos, mayúsculas |

De la conversación salieron **6 reglas de negocio** (RN-01 a RN-06),
**5 criterios de aceptación** (CA-1 a CA-5) y **7 ejemplos concretos** que se
convirtieron, casi literalmente, en los escenarios Gherkin.

### 6.2 Escenarios en Gherkin

`src/test/resources/features/login.feature` — escrito en español, sin un solo
detalle técnico, con **5 escenarios** (uno de ellos un `Esquema del escenario`
con 4 filas de `Ejemplos`, lo que da **8 ejecuciones**):

```gherkin
# language: es
@login
Característica: Inicio de sesión en el Portal de Clientes

  Antecedentes:
    Dado que el portal tiene registrado al cliente "wmelgar" con la contraseña "Segura2026!"

  @smoke @critico
  Escenario: Ingreso exitoso con credenciales válidas
    Cuando el cliente intenta ingresar con el usuario "wmelgar" y la contraseña "Segura2026!"
    Entonces el acceso es concedido
    Y el sistema muestra el mensaje "Bienvenido/a, wmelgar"

  @negativo @regresion
  Esquema del escenario: Rechazo de credenciales inválidas
    Cuando el cliente intenta ingresar con el usuario "<usuario>" y la contraseña "<contrasena>"
    Entonces el acceso es denegado
    Y el sistema muestra el mensaje "<mensaje>"
    Y le quedan <intentos> intentos disponibles

    Ejemplos: Datos incorrectos y casos de borde
      | caso                     | usuario  | contrasena  | mensaje                            | intentos |
      | Contraseña equivocada    | wmelgar  | 1234        | Credenciales inválidas             | 2        |
      | Diferencia de mayúsculas | wmelgar  | segura2026! | Credenciales inválidas             | 2        |
      | Usuario inexistente      | fantasma | Segura2026! | Credenciales inválidas             | 2        |
      | Usuario vacío            |          | Segura2026! | Debe ingresar usuario y contraseña | 3        |
```

> **Por qué un `Esquema del escenario`.** Los cuatro casos verifican **la misma
> regla** con datos distintos. Escribirlos como cuatro escenarios separados
> duplicaría el texto y escondería que se trata de una sola regla; como tabla, el
> patrón queda explícito y agregar un caso nuevo cuesta una línea.

**Etiquetas y para qué sirven:** `@smoke` (verificación rápida post-despliegue),
`@critico`, `@negativo`, `@seguridad`, `@regresion`. Permiten ejecución
selectiva: `mvn verify -Dcucumber.filter.tags="@smoke"`.

### 6.3 Step definitions

`src/test/java/cl/taller/qa/bdd/steps/PasosLogin.java`:

```java
public class PasosLogin {

    /** Sistema bajo prueba. Cucumber crea una instancia nueva por escenario. */
    private final ServicioAutenticacion servicio = new ServicioAutenticacion();
    private ResultadoAutenticacion ultimoResultado;

    @Dado("que el portal tiene registrado al cliente {string} con la contraseña {string}")
    public void registrarCliente(String usuario, String contrasena) {
        servicio.registrarUsuario(usuario, contrasena);
    }

    @Cuando("el cliente intenta ingresar con el usuario {string} y la contraseña {string}")
    public void intentarIngresar(String usuario, String contrasena) {
        ultimoResultado = servicio.autenticar(usuario, contrasena);
    }

    @Entonces("el sistema muestra el mensaje {string}")
    public void verificarMensaje(String mensajeEsperado) {
        assertEquals(mensajeEsperado, ultimoResultado.mensaje(),
            "El mensaje mostrado al usuario no corresponde al acordado con Negocio");
    }
}
```

**Buenas prácticas aplicadas**

- **Aislamiento por escenario** — Cucumber instancia la clase de pasos para cada
  escenario, por lo que el estado nace limpio. Los escenarios son atómicos, igual
  que las pruebas unitarias.
- **Capa delgada** — los pasos traducen lenguaje de negocio a llamadas del
  dominio; la lógica vive en `ServicioAutenticacion`, no en los pasos.
- **Sin lógica condicional** — ningún `if` cambia la expectativa dentro de un paso.
- **Pasos reutilizables** — un mismo método atiende `Dado` y `Cuando`, porque
  Cucumber empareja el texto y no la palabra clave.
- **Aserciones con mensaje** — ante un fallo, el reporte explica qué se esperaba.

### 6.4 Prueba de performance

`performance/login-carga.js` — **load test** del endpoint `POST /api/login`,
elegido por ser el punto de entrada de todo el portal y su primer cuello de
botella.

```javascript
export const options = {
  stages: [
    { duration: '15s', target: 10 },  // rampa: evita el "efecto avalancha"
    { duration: '30s', target: 10 },  // meseta: aquí se mide
    { duration: '10s', target: 0 },   // bajada controlada
  ],
  thresholds: {
    'http_req_duration': ['p(95)<800', 'p(99)<1500'],
    'http_req_failed':   ['rate<0.01'],
    'checks':            ['rate>0.99'],
    'http_reqs':         ['rate>5'],
  },
};
```

**Indicadores monitoreados y por qué**

| Indicador | Qué mide | Por qué importa | Medido |
|---|---|---|---|
| **TPS** | Peticiones completadas por segundo | Es la capacidad real; si cae con la misma carga, algo se degradó | **5,85 req/s** |
| **Latencia promedio** | Media de tiempos de respuesta | Referencia general; por sí sola engaña | **71,80 ms** |
| **Latencia p95** | El 95 % responde bajo este tiempo | Indicador contractual: describe al 5 % peor atendido, que es quien reclama | **89,22 ms** |
| **Latencia p99** | Cola larga | Delata pausas de GC, bloqueos y timeouts intermitentes | **330,99 ms** |
| **Tasa de error** | % de fallas técnicas | Bajo carga aparecen errores invisibles con un solo usuario | **0,00 %** |
| **Checks funcionales** | % de validaciones de negocio correctas | Responder rápido pero mal no sirve | **100 %** (984/984) |

> **El promedio esconde la cola.** En esta ejecución el promedio fue 71,8 ms y el
> p99 casi cinco veces más: 331 ms. Si solo mirásemos el promedio, la peor
> experiencia real sería invisible. Por eso el SLA se define sobre el **p95**.

> **Un hallazgo real del taller.** La primera ejecución fue **RECHAZADA** con
> 15,9 % de errores. La causa no fue el rendimiento: el script reutilizaba
> siempre el mismo usuario inválido, así que la regla RN-04 bloqueaba la cuenta
> al tercer intento y el servicio respondía 423 en vez de 401. Se corrigió
> generando un usuario distinto por iteración. La evidencia de ambas ejecuciones
> está en `docs/evidencias/`, porque **demuestra que el quality gate funciona**:
> k6 salió con código 99 y el build se habría marcado en rojo automáticamente.

### 6.5 Dashboard de métricas

Ver [`docs/02-dashboard-metricas.md`](docs/02-dashboard-metricas.md) y el
dashboard en [`reportes/index.html`](reportes/index.html), publicado por el
pipeline en GitHub Pages.

Tres niveles de visibilidad, porque responden preguntas distintas:

1. **`$GITHUB_STEP_SUMMARY`** — tabla de resultados dentro del propio run; es lo
   primero que ve quien abre el Pull Request.
2. **GitHub Pages** — dashboard HTML en una URL fija, accesible sin permisos de CI.
3. **Grafana sobre Prometheus/InfluxDB** — la serie histórica larga, donde se ven
   las tendencias de semanas y se configuran las alertas.

### 6.6 Alertas automáticas

Ver [`docs/03-alertas-automaticas.md`](docs/03-alertas-automaticas.md):
matriz de **9 alertas** con severidad, canal, destinatario y acción esperada,
más lo que deliberadamente **no** se alerta para evitar la fatiga de alertas.

El principio que ordena todo: *si una alerta no obliga a nadie a hacer algo, no
debe existir*.

---

## 7. El pipeline explicado etapa por etapa

```
  push / pull_request
         │
         ▼
  ┌──────────────────────┐
  │ 1 · Unitarias        │  mvn clean compile → mvn test
  │   ~30 s              │  publica checks + resumen + artefactos
  └──────────┬───────────┘
             │ (si falla, todo se detiene aquí)
      ┌──────┴──────┐
      ▼             ▼
┌───────────┐ ┌──────────────┐
│ 2 · BDD   │ │ 3 · k6       │   corren en paralelo: no dependen entre sí
│  Cucumber │ │  performance │
│  + mvn    │ │  umbrales    │
│    site   │ │  = gate      │
└─────┬─────┘ └──────┬───────┘
      └──────┬───────┘
             ▼
  ┌──────────────────────┐
  │ 4 · Dashboard        │  GitHub Pages (solo en main)
  └──────────┬───────────┘
             ▼
  ┌──────────────────────┐
  │ 5 · Alertas          │  if: always() — corre incluso si algo falló
  └──────────────────────┘
```

| Decisión | Justificación |
|---|---|
| Etapas ordenadas por costo | *Fail fast*: un error trivial se detecta en segundos, no en minutos |
| `cache: maven` en `setup-java` | Reutiliza `~/.m2` y reduce el build de minutos a segundos |
| `concurrency` con `cancel-in-progress` | Dos pushes seguidos cancelan el run anterior: no se gastan minutos en un commit obsoleto |
| Etapas 2 y 3 en paralelo | Son independientes entre sí; serializarlas solo alargaría el pipeline |
| `-DskipUnitTests=true` en la etapa 2 | No repite las unitarias que ya pasaron en la etapa 1 |
| `if: always()` en publicaciones y alertas | Los reportes y las alertas son *más* necesarios cuando algo falla |
| Reportes como artefactos **y** en Pages | El artefacto sirve para depurar; la URL fija, para el equipo |
| `Jenkinsfile` equivalente | Demuestra que la estrategia es independiente de la herramienta |

**Disparadores:** `push` a `main`, `feature/**` y `fix/**`; `pull_request` hacia
`main`; y `workflow_dispatch` para ejecución manual.

---

## 8. Reportes navegables

| Reporte | Generado por | Ruta | Contenido |
|---|---|---|---|
| **BDD · Cucumber** | Cucumber | `target/cucumber-reports/reporte-bdd.html` | Cada escenario con su paso a paso, duración y estado |
| **Unitarias · Surefire** | maven-surefire-report-plugin | `target/site/surefire.html` | Resumen, detalle por clase y por caso |
| **Aceptación · Failsafe** | maven-failsafe-plugin | `target/site/failsafe.html` | Resultado de la suite de integración |
| **Performance · k6** | `handleSummary()` propio | `performance/resultados/reporte-performance.html` | Indicadores frente a los umbrales del SLA |
| **Dashboard de calidad** | Proyecto | `reportes/index.html` | Vista unificada funcional + performance |

Los formatos secundarios (`cucumber.json`, `cucumber-junit.xml`,
`indicadores.json`) existen para alimentar dashboards y para que GitHub Actions y
Jenkins muestren los resultados de forma nativa.

```bash
# Regenerar todos los reportes localmente
mvn clean verify && mvn site
node performance/servidor-mock.js & k6 run performance/login-carga.js
```

---

## 9. Resultados obtenidos

Ejecución real del **29-08-2026**, JDK 17 · Maven 3.9.9 · k6 0.55.0:

| Suite | Ejecutadas | Fallos | Errores | Tiempo |
|---|---|---|---|---|
| Pruebas unitarias (Surefire) | **14** | 0 | 0 | 0,08 s |
| Escenarios BDD (Failsafe + Cucumber) | **8** | 0 | 0 | 0,30 s |
| **Total funcional** | **22** | **0** | **0** | — |

| Performance (k6) | Valor | Umbral | Resultado |
|---|---|---|---|
| Peticiones totales | 328 | — | — |
| Throughput | 5,85 TPS | > 5 | ✅ |
| Latencia promedio | 71,80 ms | < 400 ms | ✅ |
| Latencia p95 | 89,22 ms | < 800 ms | ✅ |
| Latencia p99 | 330,99 ms | < 1500 ms | ✅ |
| Tasa de error | 0,00 % | < 1 % | ✅ |
| Checks funcionales | 984/984 (100 %) | > 99 % | ✅ |

**Resultado global: pipeline en verde, todos los umbrales cumplidos.**

---

## 10. Evidencias

Todas en [`docs/evidencias/`](docs/evidencias/):

| Archivo | Qué evidencia |
|---|---|
| `01-mvn-test-local.png` | Ejecución local de las pruebas unitarias (14/14) |
| `02-mvn-verify-bdd.png` | Ejecución local de los escenarios BDD (8/8) |
| `03-reporte-bdd-cucumber.png` | Reporte navegable de Cucumber |
| `04-reporte-surefire.png` | Reporte navegable de Surefire |
| `05-reporte-performance-k6.png` | Reporte navegable de k6 |
| `06-dashboard-metricas.png` | Dashboard de calidad |
| `07-git-log-ramas.png` | Árbol de ramas, commits y merges |
| `08-estructura-proyecto.png` | Estructura de carpetas |
| `09-k6-consola.png` | Salida de consola de la prueba de carga |
| `k6-ejecucion-01-rechazada.txt` | Ejecución que **incumplió** los umbrales y su análisis |
| `k6-ejecucion-02-aprobada.txt` | Ejecución que los cumplió tras la corrección |
| `salida-*.txt` | Salidas de consola íntegras, sin editar |

Las capturas del pipeline ejecutándose en GitHub Actions se agregan tras el
primer `push` al repositorio remoto.
