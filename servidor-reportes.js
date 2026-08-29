/**
 * ---------------------------------------------------------------------------
 * Servidor estático local para previsualizar el sitio de reportes.
 * ---------------------------------------------------------------------------
 * Sirve la carpeta ./sitio, que es exactamente lo que el pipeline publica en
 * GitHub Pages. Permite revisar el dashboard y los reportes navegables antes
 * de hacer push.
 *
 * Uso:  node servidor-reportes.js      ->  http://localhost:8090
 * ---------------------------------------------------------------------------
 */
const http = require('http');
const fs = require('fs');
const path = require('path');

const PUERTO = process.env.PUERTO || 8090;
const RAIZ = path.join(__dirname, 'sitio');

const TIPOS = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.xml': 'application/xml; charset=utf-8',
  '.txt': 'text/plain; charset=utf-8',
  '.ico': 'image/x-icon',
};

http
  .createServer(function (req, res) {
    let ruta = decodeURIComponent(req.url.split('?')[0]);
    if (ruta.endsWith('/')) ruta += 'index.html';

    const destino = path.join(RAIZ, path.normalize(ruta).replace(/^(\.\.[/\\])+/, ''));

    fs.readFile(destino, function (err, datos) {
      if (err) {
        res.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' });
        return res.end('<h1>404</h1><p>No existe: ' + ruta + '</p><p><a href="/">Volver al dashboard</a></p>');
      }
      res.writeHead(200, { 'Content-Type': TIPOS[path.extname(destino).toLowerCase()] || 'application/octet-stream' });
      res.end(datos);
    });
  })
  .listen(PUERTO, function () {
    console.log('');
    console.log('  Sitio de reportes disponible en:');
    console.log('    http://localhost:' + PUERTO + '/                        Dashboard de calidad');
    console.log('    http://localhost:' + PUERTO + '/bdd/reporte-bdd.html    Reporte BDD (Cucumber)');
    console.log('    http://localhost:' + PUERTO + '/pruebas/surefire.html   Pruebas unitarias');
    console.log('    http://localhost:' + PUERTO + '/pruebas/failsafe.html   Pruebas de aceptación');
    console.log('    http://localhost:' + PUERTO + '/performance/reporte-performance.html   Performance k6');
    console.log('');
  });
