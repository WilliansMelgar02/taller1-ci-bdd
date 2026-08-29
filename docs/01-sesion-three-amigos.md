# Sesión "Three Amigos" — Funcionalidad: Inicio de sesión

**Proyecto:** Portal de Clientes
**Fecha de la sesión:** 20-08-2026 · **Duración:** 45 min
**Historia de usuario:** `HU-014 — Inicio de sesión en el portal de clientes`
**Modalidad:** Discovery Workshop previo a la planificación del sprint (Example Mapping)

---

## 1. Objetivo de la sesión

Construir un **entendimiento compartido** de la funcionalidad *antes* de escribir una sola
línea de código, y traducir ese acuerdo a **ejemplos concretos** que luego se convierten,
casi literalmente, en escenarios ejecutables de Gherkin.

La regla que ordena la sesión: **si no lo podemos expresar como un ejemplo, todavía no lo
entendemos**.

---

## 2. Roles participantes

| Rol | Participante (simulado) | Pregunta que representa | Aporte concreto en la sesión |
|---|---|---|---|
| **Negocio / Product Owner** | Carolina Rojas | *¿Qué problema resolvemos y para quién?* | Definió el valor: reducir tickets de soporte por cuentas comprometidas. Fijó el límite de 3 intentos como política de seguridad del negocio. |
| **Desarrollo** | Willians Melgar | *¿Cómo lo construimos? ¿Qué es viable?* | Advirtió que el contador de intentos debe ser por usuario y no por sesión. Propuso un servicio sin estado externo para la primera iteración. |
| **QA / Testing** | Daniela Fuentes | *¿Qué puede salir mal? ¿Cómo lo comprobamos?* | Aportó los casos de borde: usuario inexistente, campos vacíos, contraseña sensible a mayúsculas, y el comportamiento del contador tras un login exitoso. |

> **Por qué tres roles y no uno.** Cada rol trae un sesgo distinto. Negocio tiende a describir
> el camino feliz; Desarrollo piensa en la implementación; QA piensa en el fallo. La
> conversación entre los tres es lo que produce criterios de aceptación completos. El
> entregable de la sesión **no es un documento**, es el entendimiento compartido; el
> documento solo lo registra.

---

## 3. Historia de usuario acordada

> **Como** cliente registrado del portal
> **quiero** iniciar sesión con mi usuario y contraseña
> **para** acceder de forma segura a la información de mi cuenta.

---

## 4. Reglas de negocio (extraídas de la conversación)

| # | Regla | Origen |
|---|---|---|
| RN-01 | El acceso se concede solo si usuario y contraseña coinciden exactamente con los registrados. | Negocio |
| RN-02 | La contraseña distingue mayúsculas y minúsculas (*case sensitive*). | QA |
| RN-03 | Ante credenciales incorrectas el sistema informa el error **sin revelar** si falló el usuario o la contraseña. | Negocio (seguridad) |
| RN-04 | La cuenta se bloquea al acumular **3 intentos fallidos** consecutivos. | Negocio |
| RN-05 | Un ingreso exitoso **reinicia** el contador de intentos fallidos. | Desarrollo |
| RN-06 | Si usuario o contraseña vienen vacíos, se solicitan ambos datos sin descontar intentos. | QA |

### Preguntas abiertas registradas (no bloquean el sprint)

- ¿El bloqueo se libera solo tras 15 minutos o requiere intervención de soporte? → *Pendiente con el área de Seguridad; para esta iteración el bloqueo es permanente hasta desbloqueo manual.*
- ¿Se notifica por correo el bloqueo? → *Fuera del alcance de HU-014, se crea HU-021.*

---

## 5. Criterios de aceptación

- **CA-1** — Dado un usuario registrado, cuando ingresa credenciales correctas, entonces obtiene acceso y un saludo personalizado.
- **CA-2** — Dado un usuario registrado, cuando ingresa credenciales incorrectas, entonces el acceso es denegado con un mensaje genérico y se descuenta un intento.
- **CA-3** — Dado un usuario que ya falló 2 veces, cuando falla una tercera vez, entonces la cuenta queda bloqueada.
- **CA-4** — Dado un usuario bloqueado, cuando ingresa la contraseña correcta, entonces el acceso sigue denegado.
- **CA-5** — Dado un usuario que falló 2 veces, cuando ingresa correctamente, entonces accede y su contador vuelve a 3 intentos disponibles.

---

## 6. Ejemplos discutidos en la sesión (Example Mapping)

Los ejemplos se escribieron en la pizarra en formato tabla y se convirtieron directamente en
el `Esquema del escenario` del archivo `login.feature`.

| # | Usuario | Contraseña | Resultado esperado | Regla | ¿Automatizado? |
|---|---|---|---|---|---|
| E1 | `wmelgar` | `Segura2026!` | Acceso concedido | RN-01 | Sí — `Escenario` |
| E2 | `wmelgar` | `segura2026!` | Denegado (case sensitive) | RN-02 | Sí — `Ejemplos` |
| E3 | `wmelgar` | `1234` | Denegado, quedan 2 intentos | RN-03/04 | Sí — `Ejemplos` |
| E4 | `fantasma` | `Segura2026!` | Denegado, mismo mensaje genérico | RN-03 | Sí — `Ejemplos` |
| E5 | *(vacío)* | `Segura2026!` | "Debe ingresar usuario y contraseña" | RN-06 | Sí — `Ejemplos` |
| E6 | `wmelgar` | 3 fallos seguidos | Cuenta bloqueada | RN-04 | Sí — `Escenario` |
| E7 | `wmelgar` | 2 fallos + acierto | Acceso y contador reiniciado | RN-05 | Sí — `Escenario` |

---

## 7. Acuerdos de cierre

1. Los ejemplos de la tabla se automatizan como escenarios Gherkin en `src/test/resources/features/login.feature`.
2. Los escenarios se escriben en **lenguaje de negocio**: nada de selectores, IDs ni detalles técnicos dentro del `.feature`.
3. La historia se considera **terminada (DoD)** solo cuando los escenarios corren verdes en el pipeline de CI.
4. El escenario E1 se etiqueta `@smoke` para poder ejecutarlo como verificación rápida post-despliegue.
