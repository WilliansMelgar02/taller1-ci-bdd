# language: es
# ---------------------------------------------------------------------------
# PRUEBAS DE ACEPTACIÓN — Acceptance Gate del deployment pipeline.
#
# No se ejecutan contra el código en memoria, sino contra la aplicación ya
# desplegada en el ambiente de pruebas (contenedor Docker). Cubren solo los
# escenarios críticos de punta a punta: navegador → interfaz → API → servicio.
# Las reglas de negocio en detalle ya las validan los escenarios BDD de
# features/login.feature, que son más rápidos y baratos.
# ---------------------------------------------------------------------------
@aceptacion @web
Característica: Ingreso al Portal de Clientes desde el navegador

  Como cliente del portal
  quiero ingresar desde mi navegador
  para confirmar que la versión desplegada se puede usar de verdad.

  Antecedentes:
    Dado que el cliente abre el Portal de Clientes en el navegador

  @smoke
  Escenario: El portal está disponible y muestra la versión desplegada
    Entonces el portal muestra la versión que se está desplegando

  @critico
  Escenario: Un cliente registrado ingresa al portal
    Cuando ingresa con el usuario "cliente1" y la contraseña "Clave123!"
    Entonces ve el mensaje de éxito "Bienvenido/a, cliente1"

  @critico @seguridad
  Escenario: Un visitante no registrado es rechazado con un mensaje genérico
    Cuando ingresa con un usuario no registrado
    Entonces ve el mensaje de error "Credenciales inválidas"

  Escenario: El portal pide completar los datos antes de validar
    Cuando ingresa con el usuario "cliente1" sin escribir la contraseña
    Entonces ve el mensaje de error "Debe ingresar usuario y contraseña"
