/**
 * ---------------------------------------------------------------------------
 * Prueba de performance del endpoint POST /api/login  —  herramienta: k6
 * ---------------------------------------------------------------------------
 * Funcionalidad clave elegida: el LOGIN. Es el punto de entrada de todo el
 * portal, por lo que concentra el mayor volumen de peticiones y es el primer
 * cuello de botella que percibe el usuario. Si el login se degrada, ninguna
 * otra funcionalidad importa.
 *
 * Tipo de prueba: LOAD TEST (carga esperada), no stress test. Buscamos
 * confirmar que el sistema cumple el acuerdo de nivel de servicio bajo la
 * carga normal de un día hábil, no encontrar su punto de quiebre.
 *
 * Ejecución:
 *   node performance/servidor-mock.js &
 *   k6 run performance/login-carga.js
 * ---------------------------------------------------------------------------
 */
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// ---------------------------------------------------------------------------
// MÉTRICAS PERSONALIZADAS
// Además de las métricas nativas de k6, se declaran métricas de negocio para
// poder graficarlas por separado en el dashboard.
// ---------------------------------------------------------------------------
const latenciaLogin    = new Trend('latencia_login_ms', true);  // distribución de tiempos
const tasaErrorNegocio = new Rate('errores_negocio');           // % de respuestas no esperadas
const loginsExitosos   = new Counter('logins_exitosos');        // throughput útil
const loginsRechazados = new Counter('logins_rechazados');      // credenciales inválidas

const URL_BASE = __ENV.URL_BASE || 'http://localhost:8088';

// ---------------------------------------------------------------------------
// CLASIFICACIÓN DE RESPUESTAS
// Un 401 ante credenciales inválidas es la respuesta CORRECTA del sistema, no
// un error del servidor. Se declara como "estado esperado" para que no
// contamine la métrica http_req_failed, que debe reflejar únicamente fallas
// técnicas reales (5xx, timeouts, conexiones caídas).
//
// Nota: desde k6 v0.34 esto se registra con http.setResponseCallback() y NO
// como una clave dentro de "options"; ahí k6 la ignora silenciosamente.
// ---------------------------------------------------------------------------
http.setResponseCallback(http.expectedStatuses(200, 401));

export const options = {
  // -------------------------------------------------------------------------
  // PERFIL DE CARGA: rampa de subida, meseta y bajada.
  // La rampa evita el "efecto avalancha" (todos los usuarios de golpe), que no
  // representa el comportamiento real y distorsiona las mediciones.
  // -------------------------------------------------------------------------
  stages: [
    { duration: '15s', target: 10 },  // rampa: 0 -> 10 usuarios virtuales
    { duration: '30s', target: 10 },  // meseta: carga sostenida (aquí se mide)
    { duration: '10s', target: 0 },   // bajada controlada
  ],

  // -------------------------------------------------------------------------
  // UMBRALES (thresholds) = criterios de aceptación de performance.
  // Si alguno se incumple, k6 termina con código de salida 99 y el pipeline
  // marca el build como FALLIDO. Esto convierte el SLA en un "quality gate"
  // automático, y no en una recomendación que alguien deba revisar a mano.
  // -------------------------------------------------------------------------
  thresholds: {
    'http_req_duration': ['p(95)<800', 'p(99)<1500'], // latencia percentil 95 y 99
    'http_req_failed':   ['rate<0.01'],               // menos del 1% de errores técnicos
    'errores_negocio':   ['rate<0.01'],               // menos del 1% de errores de negocio
    'checks':            ['rate>0.99'],               // 99% de validaciones correctas
    'http_reqs':         ['rate>5'],                  // throughput mínimo: 5 TPS
    'latencia_login_ms': ['avg<400', 'p(95)<800'],
  },

  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

/**
 * Mezcla de tráfico realista: 4 de cada 5 ingresos son exitosos.
 *
 * El usuario inválido es distinto en cada iteración (VU + ITER) para que la
 * regla de bloqueo por 3 intentos fallidos (RN-04) no se dispare y contamine
 * la medición: cada petición debe ser independiente de las anteriores.
 */
function credencialesDelTurno() {
  const esCorrecto = Math.random() < 0.8;
  return esCorrecto
    ? { usuario: 'wmelgar', contrasena: 'Segura2026!', esperado: 200 }
    : { usuario: 'intruso-' + __VU + '-' + __ITER, contrasena: 'clave-mala', esperado: 401 };
}

export default function () {
  group('Inicio de sesión en el portal', function () {
    const credenciales = credencialesDelTurno();

    const respuesta = http.post(
      URL_BASE + '/api/login',
      JSON.stringify({ usuario: credenciales.usuario, contrasena: credenciales.contrasena }),
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { operacion: 'login', tipo: credenciales.esperado === 200 ? 'valido' : 'invalido' },
      }
    );

    latenciaLogin.add(respuesta.timings.duration);

    // Las validaciones funcionales también corren bajo carga: no basta con que
    // el sistema responda rápido, tiene que responder BIEN.
    const validaciones = check(respuesta, {
      'el código HTTP es el esperado': function (r) {
        return r.status === credenciales.esperado;
      },
      'la respuesta llega en menos de 800 ms': function (r) {
        return r.timings.duration < 800;
      },
      'el cuerpo trae mensaje o token': function (r) {
        return !!r.body && (r.body.indexOf('mensaje') >= 0 || r.body.indexOf('token') >= 0);
      },
    });

    tasaErrorNegocio.add(!validaciones);

    if (respuesta.status === 200) {
      loginsExitosos.add(1);
    } else if (respuesta.status === 401) {
      loginsRechazados.add(1);
    }
  });

  // Tiempo de reflexión del usuario entre acciones (think time).
  // Sin él, la prueba mide la capacidad máquina-a-máquina y no el uso real.
  sleep(Math.random() * 1.5 + 0.5);
}

// ---------------------------------------------------------------------------
// REPORTE: k6 entrega los datos crudos; aquí se generan los artefactos que
// consume el pipeline (JSON para el dashboard, HTML navegable para el equipo).
// ---------------------------------------------------------------------------
export function handleSummary(data) {
  const m = data.metrics;

  function valor(metrica, campo) {
    return m[metrica] && m[metrica].values[campo] != null ? m[metrica].values[campo] : 0;
  }

  const indicadores = {
    fecha: new Date().toISOString(),
    peticiones_totales: valor('http_reqs', 'count'),
    tps: valor('http_reqs', 'rate'),
    latencia_promedio_ms: valor('http_req_duration', 'avg'),
    latencia_p95_ms: valor('http_req_duration', 'p(95)'),
    latencia_p99_ms: valor('http_req_duration', 'p(99)'),
    latencia_max_ms: valor('http_req_duration', 'max'),
    tasa_error: valor('http_req_failed', 'rate'),
    logins_exitosos: valor('logins_exitosos', 'count'),
    logins_rechazados: valor('logins_rechazados', 'count'),
    checks_ok: valor('checks', 'rate'),
    usuarios_maximos: valor('vus_max', 'value'),
  };

  const umbralesIncumplidos = [];
  Object.keys(m).forEach(function (nombre) {
    const th = m[nombre].thresholds;
    if (!th) return;
    Object.keys(th).forEach(function (expr) {
      if (th[expr].ok === false) umbralesIncumplidos.push(nombre + ': ' + expr);
    });
  });

  function n(x, d) {
    return Number(x).toFixed(d === undefined ? 2 : d);
  }

  const estado = umbralesIncumplidos.length === 0 ? 'APROBADA' : 'RECHAZADA';
  const detalleUmbrales = umbralesIncumplidos.length === 0 ? 'ninguno' : umbralesIncumplidos.join(' | ');

  const texto =
    '\n==========================================================\n' +
    '  PRUEBA DE PERFORMANCE - LOGIN        Resultado: ' + estado + '\n' +
    '==========================================================\n' +
    '  Peticiones totales .......... ' + indicadores.peticiones_totales + '\n' +
    '  Throughput (TPS) ............ ' + n(indicadores.tps) + ' req/s\n' +
    '  Latencia promedio ........... ' + n(indicadores.latencia_promedio_ms) + ' ms\n' +
    '  Latencia p95 ................ ' + n(indicadores.latencia_p95_ms) + ' ms   (umbral < 800)\n' +
    '  Latencia p99 ................ ' + n(indicadores.latencia_p99_ms) + ' ms   (umbral < 1500)\n' +
    '  Latencia maxima ............. ' + n(indicadores.latencia_max_ms) + ' ms\n' +
    '  Tasa de error ............... ' + n(indicadores.tasa_error * 100) + ' %    (umbral < 1%)\n' +
    '  Checks correctos ............ ' + n(indicadores.checks_ok * 100) + ' %    (umbral > 99%)\n' +
    '  Logins exitosos / rechazados  ' + indicadores.logins_exitosos + ' / ' + indicadores.logins_rechazados + '\n' +
    '  Usuarios virtuales maximos .. ' + indicadores.usuarios_maximos + '\n' +
    '----------------------------------------------------------\n' +
    '  Umbrales incumplidos: ' + detalleUmbrales + '\n' +
    '==========================================================\n';

  function fila(etiqueta, valorTexto, umbral, ok) {
    return '<tr><td>' + etiqueta + '</td><td class="valor">' + valorTexto + '</td><td>' + umbral +
      '</td><td class="' + (ok ? 'ok' : 'falla') + '">' + (ok ? 'CUMPLE' : 'NO CUMPLE') + '</td></tr>';
  }

  const html = '<!doctype html>\n<html lang="es"><head><meta charset="utf-8">\n' +
    '<title>Reporte de Performance - Login</title>\n<style>\n' +
    'body{font-family:Segoe UI,Arial,sans-serif;margin:0;background:#f4f6f8;color:#1f2933}\n' +
    'header{background:#1f3a5f;color:#fff;padding:24px 32px}\n' +
    'header h1{margin:0;font-size:22px} header p{margin:6px 0 0;opacity:.85;font-size:13px}\n' +
    '.estado{display:inline-block;margin-top:12px;padding:6px 14px;border-radius:4px;font-weight:700;color:#fff;' +
    'background:' + (estado === 'APROBADA' ? '#1b7f4b' : '#b3261e') + '}\n' +
    'main{padding:24px 32px;max-width:1000px}\n' +
    '.tarjetas{display:flex;flex-wrap:wrap;gap:14px;margin-bottom:24px}\n' +
    '.tarjeta{background:#fff;border:1px solid #dfe3e8;border-radius:6px;padding:14px 18px;min-width:150px}\n' +
    '.tarjeta span{display:block;font-size:11px;text-transform:uppercase;color:#6b7684;letter-spacing:.5px}\n' +
    '.tarjeta strong{font-size:24px;color:#1f3a5f}\n' +
    'table{width:100%;border-collapse:collapse;background:#fff;border:1px solid #dfe3e8;border-radius:6px;overflow:hidden}\n' +
    'th,td{padding:10px 14px;text-align:left;border-bottom:1px solid #eceff1;font-size:14px}\n' +
    'th{background:#eef2f6;font-size:12px;text-transform:uppercase;color:#52606d}\n' +
    '.valor{font-weight:600} .ok{color:#1b7f4b;font-weight:700} .falla{color:#b3261e;font-weight:700}\n' +
    'footer{padding:16px 32px;font-size:12px;color:#6b7684}\n' +
    '</style></head><body>\n<header>\n' +
    '<h1>Reporte de Performance &mdash; Endpoint POST /api/login</h1>\n' +
    '<p>Generado por k6 el ' + new Date().toLocaleString('es-CL') + ' &middot; Taller 1, Unidad II</p>\n' +
    '<div class="estado">' + estado + '</div>\n</header>\n<main>\n' +
    '<div class="tarjetas">\n' +
    '<div class="tarjeta"><span>Throughput</span><strong>' + n(indicadores.tps) + '</strong> TPS</div>\n' +
    '<div class="tarjeta"><span>Latencia p95</span><strong>' + n(indicadores.latencia_p95_ms, 0) + '</strong> ms</div>\n' +
    '<div class="tarjeta"><span>Latencia media</span><strong>' + n(indicadores.latencia_promedio_ms, 0) + '</strong> ms</div>\n' +
    '<div class="tarjeta"><span>Tasa de error</span><strong>' + n(indicadores.tasa_error * 100) + '</strong> %</div>\n' +
    '<div class="tarjeta"><span>Peticiones</span><strong>' + indicadores.peticiones_totales + '</strong></div>\n' +
    '<div class="tarjeta"><span>VUs maximos</span><strong>' + indicadores.usuarios_maximos + '</strong></div>\n' +
    '</div>\n<table>\n' +
    '<tr><th>Indicador</th><th>Valor medido</th><th>Umbral (SLA)</th><th>Resultado</th></tr>\n' +
    fila('Latencia p95', n(indicadores.latencia_p95_ms) + ' ms', '&lt; 800 ms', indicadores.latencia_p95_ms < 800) + '\n' +
    fila('Latencia p99', n(indicadores.latencia_p99_ms) + ' ms', '&lt; 1500 ms', indicadores.latencia_p99_ms < 1500) + '\n' +
    fila('Tasa de error tecnico', n(indicadores.tasa_error * 100) + ' %', '&lt; 1 %', indicadores.tasa_error < 0.01) + '\n' +
    fila('Throughput', n(indicadores.tps) + ' req/s', '&gt; 5 req/s', indicadores.tps > 5) + '\n' +
    fila('Checks funcionales', n(indicadores.checks_ok * 100) + ' %', '&gt; 99 %', indicadores.checks_ok > 0.99) + '\n' +
    '</table>\n</main>\n' +
    '<footer>Umbrales incumplidos: ' + detalleUmbrales + '</footer>\n' +
    '</body></html>';

  return {
    stdout: texto,
    'performance/resultados/resumen-k6.json': JSON.stringify(data, null, 2),
    'performance/resultados/indicadores.json': JSON.stringify(indicadores, null, 2),
    'performance/resultados/reporte-performance.html': html,
  };
}
