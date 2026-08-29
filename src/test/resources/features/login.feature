# language: es
# ---------------------------------------------------------------------------
# Especificación ejecutable de la HU-014 "Inicio de sesión".
# Redactada en lenguaje de negocio durante la sesión Three Amigos
# (ver docs/01-sesion-three-amigos.md). No contiene detalles técnicos:
# ni selectores, ni URLs, ni nombres de clases. Cualquier persona del
# equipo puede leerla y validarla.
# ---------------------------------------------------------------------------
@login
Característica: Inicio de sesión en el Portal de Clientes

  Como cliente registrado del portal
  quiero iniciar sesión con mi usuario y contraseña
  para acceder de forma segura a la información de mi cuenta.

  # Contexto común a todos los escenarios. Se ejecuta antes de cada uno,
  # lo que garantiza que cada escenario parta desde un estado limpio.
  Antecedentes:
    Dado que el portal tiene registrado al cliente "wmelgar" con la contraseña "Segura2026!"

  @smoke @critico
  Escenario: Ingreso exitoso con credenciales válidas
    Cuando el cliente intenta ingresar con el usuario "wmelgar" y la contraseña "Segura2026!"
    Entonces el acceso es concedido
    Y el sistema muestra el mensaje "Bienvenido/a, wmelgar"

  # Esquema del escenario = Scenario Outline. Un mismo comportamiento
  # verificado con varios juegos de datos: cada fila de "Ejemplos" es un
  # escenario independiente con su propio estado.
  @negativo @regresion
  Esquema del escenario: Rechazo de credenciales inválidas
    Cuando el cliente intenta ingresar con el usuario "<usuario>" y la contraseña "<contrasena>"
    Entonces el acceso es denegado
    Y el sistema muestra el mensaje "<mensaje>"
    Y le quedan <intentos> intentos disponibles

    Ejemplos: Datos incorrectos y casos de borde
      | caso                       | usuario  | contrasena   | mensaje                            | intentos |
      | Contraseña equivocada      | wmelgar  | 1234         | Credenciales inválidas             | 2        |
      | Diferencia de mayúsculas   | wmelgar  | segura2026!  | Credenciales inválidas             | 2        |
      | Usuario inexistente        | fantasma | Segura2026!  | Credenciales inválidas             | 2        |
      | Usuario vacío              |          | Segura2026!  | Debe ingresar usuario y contraseña | 3        |

  @seguridad @critico
  Escenario: Bloqueo de la cuenta tras tres intentos fallidos consecutivos
    Cuando el cliente falla el ingreso 3 veces seguidas
    Entonces la cuenta del cliente "wmelgar" queda bloqueada
    Y el sistema muestra el mensaje "Cuenta bloqueada por múltiples intentos fallidos"

  @seguridad
  Escenario: La cuenta bloqueada no acepta ni siquiera la contraseña correcta
    Dado que el cliente falla el ingreso 3 veces seguidas
    Cuando el cliente intenta ingresar con el usuario "wmelgar" y la contraseña "Segura2026!"
    Entonces el acceso es denegado
    Y el sistema muestra el mensaje "Cuenta bloqueada por múltiples intentos fallidos"

  @regresion
  Escenario: Un ingreso exitoso reinicia el contador de intentos fallidos
    Dado que el cliente falla el ingreso 2 veces seguidas
    Cuando el cliente intenta ingresar con el usuario "wmelgar" y la contraseña "Segura2026!"
    Entonces el acceso es concedido
    Y le quedan 3 intentos disponibles
