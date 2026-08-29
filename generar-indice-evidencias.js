/**
 * ---------------------------------------------------------------------------
 * Genera el índice navegable de las evidencias del taller.
 * ---------------------------------------------------------------------------
 * Produce docs/evidencias/index.html: una página que muestra cada captura
 * junto a la descripción de lo que debe demostrar y la figura del informe en
 * la que aparece.
 *
 * ¿Por qué existe? Una carpeta con dieciocho PNG no dice nada por sí sola.
 * Este índice hace verificable la afirmación "la evidencia respalda lo que
 * el informe sostiene": cualquiera puede abrirlo y contrastar, una por una,
 * qué demuestra cada imagen. Es trazabilidad, no decoración.
 *
 * Uso:  node generar-indice-evidencias.js
 * ---------------------------------------------------------------------------
 */
const fs = require('fs');
const path = require('path');

const CARPETA = path.join(__dirname, 'docs', 'evidencias');
const SALIDA = path.join(CARPETA, 'index.html');

// Qué demuestra cada evidencia y en qué figura del informe aparece.
const ESPERADO = {
  '01-mvn-test-local.png':            ['Fig. 2',  'Ejecución local de <code>mvn clean test</code>: las cuatro suites de Calculadora, <b>Tests run: 28</b> y <b>BUILD SUCCESS</b>'],
  '02-mvn-verify-bdd.png':            ['Fig. 7',  'Los ocho escenarios BDD en español con sus acentos correctos, <b>Tests run: 8</b> y <b>BUILD SUCCESS</b>'],
  '02b-escenarios-bdd-smoke.png':     ['Fig. 8',  'Ejecución selectiva con el filtro <code>@smoke</code>: <b>Skipped: 7</b>, es decir un solo escenario ejecutado'],
  '03-reporte-bdd-cucumber.png':      ['Fig. 11', 'Reporte de Cucumber: <b>8 PASSED</b>, con los escenarios expandidos y la tabla de Ejemplos del Esquema del escenario'],
  '04-reporte-surefire.png':          ['Fig. 5',  'Reporte de Surefire con las <b>28</b> pruebas unitarias, 0 errores y 100 % de éxito'],
  '05-reporte-performance-k6.png':    ['Fig. 13', 'Reporte de k6 con estado <b>APROBADA</b> y cada indicador contrastado con su umbral'],
  '07-git-log-ramas.png':             ['Fig. 1',  'Ramas locales y remotas, y el árbol de commits con los merges <b>PR #3 a #7</b>'],
  '08-estructura-proyecto.png':       ['Fig. 3',  'Estructura del proyecto versionado, sin <code>target/</code> ni <code>.git/</code>'],
  '09-k6-consola.png':                ['Fig. 12', 'Prueba de carga: <b>APROBADA</b>, 328 peticiones, <b>5,91 TPS</b>, p95 <b>93,90 ms</b> y 0 % de error'],
  '11-reporte-failsafe.png':          ['Fig. 6',  'Reporte de Failsafe con los <b>8</b> escenarios de la suite de aceptación'],
  '12-github-actions-pipeline.png':   ['Fig. 9',  'Pipeline en GitHub Actions: las <b>cinco etapas en verde</b> y el detalle de las pruebas unitarias'],
  '13-github-actions-metricas.png':   ['Fig. 10', 'Métricas publicadas por el run: escenarios BDD, <b>cobertura JaCoCo</b> y los seis indicadores de performance'],
  '14-dashboard-publicado.png':       ['Fig. 14', 'Dashboard en GitHub Pages: <b>28/28</b> unitarias, <b>8/8</b> BDD, <b>cobertura 99,1 %</b>, p95 <b>94 ms</b> y <b>5,91 TPS</b>'],
  '14b-dashboard-datos.png':          ['Fig. 15', 'Evolución del percentil 95 por build, la vista de datos en tabla y los enlaces a los reportes navegables'],
  '15-github-actions-artefactos.png': ['Fig. 16', 'Etapa <b>5 · Alertas automáticas</b> y los cuatro artefactos publicados por el pipeline'],
  '16-reporte-cobertura.png':         ['Fig. 4',  'Reporte de JaCoCo con el detalle por clase: <b>Calculadora al 100 %</b> tras cubrir los métodos que faltaban'],
  '12-completo-github-actions.jpeg':  ['—',       'Página completa del run, de la que se recortan las figuras 9, 10 y 16'],
  '14-completo-dashboard.jpeg':       ['—',       'Página completa del dashboard, de la que se recortan las figuras 14 y 15'],
};

const archivos = fs.readdirSync(CARPETA)
  .filter((f) => /\.(png|jpe?g)$/i.test(f))
  .sort();

const fichas = archivos.map((f, i) => {
  const [figura, demuestra] = ESPERADO[f] || ['—', 'Sin descripción registrada'];
  const kb = Math.round(fs.statSync(path.join(CARPETA, f)).size / 1024);
  return `
  <section class="ficha">
    <header>
      <span class="n">${i + 1}</span>
      <div class="titulo">
        <h2>${f}</h2>
        <p class="meta">${figura} del informe &middot; ${kb} KB</p>
      </div>
    </header>
    <p class="demuestra"><strong>Demuestra:</strong> ${demuestra}</p>
    <a href="${encodeURIComponent(f)}" target="_blank" rel="noopener">
      <img src="${encodeURIComponent(f)}" alt="${f}" loading="lazy">
    </a>
  </section>`;
}).join('\n');

const html = `<!doctype html>
<html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Índice de evidencias · Taller 1</title>
<style>
  :root{
    color-scheme: light;
    --plano:#f9f9f7; --superficie:#fcfcfb; --borde:#e1e0d9;
    --tinta:#0b0b0b; --tinta-2:#52514e; --tinta-mute:#898781; --acento:#1f3a5f;
  }
  @media (prefers-color-scheme: dark){
    :root{
      color-scheme: dark;
      --plano:#0d0d0d; --superficie:#1a1a19; --borde:#2c2c2a;
      --tinta:#ffffff; --tinta-2:#c3c2b7; --tinta-mute:#898781; --acento:#3987e5;
    }
  }
  *{box-sizing:border-box}
  body{margin:0;background:var(--plano);color:var(--tinta);
       font:15px/1.55 "Segoe UI",system-ui,-apple-system,Arial,sans-serif}
  header.top{background:var(--acento);color:#fff;padding:26px 32px}
  header.top h1{margin:0 0 5px;font-size:21px;letter-spacing:-.2px}
  header.top p{margin:0;opacity:.88;font-size:13.5px;max-width:70ch}
  header.top a{color:#fff}
  main{max-width:1080px;margin:0 auto;padding:26px 20px 60px}
  .ficha{background:var(--superficie);border:1px solid var(--borde);
         border-radius:8px;margin-bottom:22px;overflow:hidden}
  .ficha header{display:flex;align-items:center;gap:14px;padding:14px 18px;
                border-bottom:1px solid var(--borde)}
  .n{background:var(--acento);color:#fff;width:30px;height:30px;border-radius:50%;
     display:grid;place-items:center;font-weight:700;font-size:14px;flex:none}
  .ficha h2{margin:0;font-size:14.5px;font-family:Consolas,ui-monospace,monospace;word-break:break-all}
  .meta{margin:2px 0 0;font-size:12px;color:var(--tinta-mute)}
  .demuestra{margin:0;padding:12px 18px;font-size:13.5px;color:var(--tinta-2);
             border-bottom:1px solid var(--borde)}
  code{font-family:Consolas,ui-monospace,monospace;font-size:.92em;
       background:var(--plano);padding:1px 5px;border-radius:3px}
  .ficha img{display:block;width:100%;height:auto}
  footer{text-align:center;color:var(--tinta-mute);font-size:12.5px;padding:22px}
</style></head><body>
<header class="top">
  <h1>Índice de evidencias &mdash; Taller 1, Unidad II</h1>
  <p>Las ${archivos.length} capturas que respaldan el informe, cada una junto a lo que demuestra y a la figura
     en la que aparece. Todas corresponden a ejecuciones reales.
     <a href="../../">Volver al dashboard</a></p>
</header>
<main>
${fichas}
</main>
<footer>Taller 1 &middot; Evaluación Unidad II &middot; Willians Melgar</footer>
</body></html>`;

fs.writeFileSync(SALIDA, html, 'utf8');
console.log('Índice generado con ' + archivos.length + ' evidencias: ' + SALIDA);
