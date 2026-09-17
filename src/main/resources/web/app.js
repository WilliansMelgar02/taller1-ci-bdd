'use strict';

/*
 * Comportamiento del formulario de ingreso.
 *
 * El estado del mensaje se expone en el atributo data-estado
 * (inicial | pendiente | exito | error). Las pruebas de aceptación esperan a
 * que deje de ser "pendiente" en lugar de usar pausas fijas: una espera
 * explícita es más rápida y no produce pruebas intermitentes.
 */

function mostrarMensaje(texto, estado) {
  const mensaje = document.getElementById('mensaje');
  mensaje.textContent = texto;
  mensaje.dataset.estado = estado;
}

async function ingresar(evento) {
  evento.preventDefault();
  mostrarMensaje('Validando credenciales…', 'pendiente');

  try {
    const respuesta = await fetch('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        usuario: document.getElementById('usuario').value,
        clave: document.getElementById('contrasena').value,
      }),
    });
    const datos = await respuesta.json();
    mostrarMensaje(datos.mensaje, respuesta.ok ? 'exito' : 'error');
  } catch (error) {
    mostrarMensaje('No fue posible contactar al servidor', 'error');
  }
}

/* Muestra qué versión y qué color Blue-Green atienden la página: es la
   evidencia visible de hacia dónde apunta el tráfico tras un despliegue. */
async function mostrarVersion() {
  const pie = document.getElementById('version-desplegada');
  try {
    const salud = await (await fetch('/health')).json();
    pie.textContent = 'Versión ' + salud.version + ' · commit ' + salud.commit.substring(0, 7) +
      ' · entorno ' + salud.color;
    pie.dataset.version = salud.version;
    pie.dataset.color = salud.color;
  } catch (error) {
    pie.textContent = 'Versión no disponible';
  }
}

document.addEventListener('DOMContentLoaded', function () {
  document.getElementById('formulario-login').addEventListener('submit', ingresar);
  mostrarVersion();
});
