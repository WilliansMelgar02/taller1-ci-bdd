/**
 * ---------------------------------------------------------------------------
 * Servidor mock del endpoint POST /api/login
 * ---------------------------------------------------------------------------
 * Reproduce el contrato del servicio real (mismos códigos HTTP, mismo cuerpo
 * JSON) y aplica las reglas de negocio acordadas en la sesión Three Amigos.
 *
 * ¿Por qué un mock y no el servicio real?
 *   1. La prueba de carga debe medir el rendimiento de forma REPETIBLE. Un
 *      backend compartido introduce ruido (otros usuarios, otras cargas).
 *   2. Permite ejecutar la prueba dentro del pipeline, sin credenciales ni
 *      infraestructura externa.
 *   3. Incluye una latencia base simulada para que la prueba tenga un
 *      comportamiento realista y los umbrales sean significativos.
 *
 * Uso:  node performance/servidor-mock.js     (escucha en http://localhost:8088)
 * ---------------------------------------------------------------------------
 */
const http = require('http');

const PUERTO = process.env.PUERTO_MOCK || 8088;

// Repositorio de usuarios en memoria (equivalente a ServicioAutenticacion.java)
const USUARIOS = {
  wmelgar: 'Segura2026!',
  cliente1: 'Clave123!',
  cliente2: 'Clave123!',
};

const MAX_INTENTOS = 3;
const intentosFallidos = new Map();

/** Latencia base simulada del backend, en milisegundos. */
function latenciaSimulada() {
  // Distribución realista: la mayoría rápida, con una cola larga ocasional.
  const base = 35 + Math.random() * 45;          // 35-80 ms habitual
  const colaLarga = Math.random() < 0.03 ? 250 : 0; // 3% de peticiones lentas
  return base + colaLarga;
}

const servidor = http.createServer((req, res) => {
  if (req.method === 'GET' && req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ estado: 'arriba' }));
  }

  if (req.method !== 'POST' || req.url !== '/api/login') {
    res.writeHead(404, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ mensaje: 'Recurso no encontrado' }));
  }

  let cuerpo = '';
  req.on('data', (fragmento) => (cuerpo += fragmento));
  req.on('end', () => {
    setTimeout(() => {
      let usuario;
      let contrasena;
      try {
        ({ usuario, contrasena } = JSON.parse(cuerpo || '{}'));
      } catch (e) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ mensaje: 'JSON inválido' }));
      }

      // RN-06: campos vacíos
      if (!usuario || !contrasena) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ mensaje: 'Debe ingresar usuario y contraseña' }));
      }

      const fallos = intentosFallidos.get(usuario) || 0;

      // RN-04: cuenta bloqueada
      if (fallos >= MAX_INTENTOS) {
        res.writeHead(423, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({
          mensaje: 'Cuenta bloqueada por múltiples intentos fallidos',
          intentosRestantes: 0,
        }));
      }

      // RN-01 / RN-02: comparación exacta
      if (USUARIOS[usuario] === contrasena) {
        intentosFallidos.delete(usuario);   // RN-05
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({
          token: 'jwt-simulado-' + Date.now(),
          mensaje: 'Bienvenido/a, ' + usuario,
          intentosRestantes: MAX_INTENTOS,
        }));
      }

      // RN-03: mensaje genérico
      intentosFallidos.set(usuario, fallos + 1);
      res.writeHead(401, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({
        mensaje: 'Credenciales inválidas',
        intentosRestantes: MAX_INTENTOS - (fallos + 1),
      }));
    }, latenciaSimulada());
  });
});

servidor.listen(PUERTO, () => {
  console.log(`[mock] Servicio de login escuchando en http://localhost:${PUERTO}`);
});
