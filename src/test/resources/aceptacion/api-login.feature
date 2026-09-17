# language: es
# ---------------------------------------------------------------------------
# PRUEBAS DE ACEPTACIÓN — contrato HTTP del servicio desplegado.
#
# El escenario de salud es el primero en fallar si el despliegue quedó a
# medias: confirma que responde la versión recién desplegada y no la anterior.
# ---------------------------------------------------------------------------
@aceptacion @api
Característica: Contrato de la API de login en el ambiente desplegado

  Como sistema que consume el servicio de autenticación
  quiero que la API desplegada respete su contrato
  para integrarme con ella sin sorpresas.

  @smoke
  Escenario: El servicio está operativo con la versión desplegada
    Cuando se consulta el estado de salud del servicio
    Entonces el servicio responde que está operativo
    Y el servicio informa la versión que se está desplegando

  @critico
  Escenario: La API concede el acceso a un cliente registrado
    Cuando se solicita el ingreso por la API con el usuario "cliente2" y la contraseña "Clave123!"
    Entonces la API responde con el código 200
    Y el mensaje de la respuesta es "Bienvenido/a, cliente2"

  @seguridad
  Escenario: La API rechaza a un usuario no registrado sin revelar el motivo
    Cuando se solicita el ingreso por la API con un usuario no registrado
    Entonces la API responde con el código 401
    Y el mensaje de la respuesta es "Credenciales inválidas"

  Escenario: La API exige usuario y contraseña
    Cuando se solicita el ingreso por la API con el usuario "" y la contraseña "Clave123!"
    Entonces la API responde con el código 400
    Y el mensaje de la respuesta es "Debe ingresar usuario y contraseña"
