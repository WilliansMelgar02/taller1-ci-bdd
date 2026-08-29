# Métricas y dashboard del pipeline

> Cómo se agregan las métricas de pruebas **funcionales** y de **performance** a un
> dashboard visible para todo el equipo, y qué decisiones se toman con cada una.

---

## 1. El problema que resuelve un dashboard

Un pipeline que solo responde "verde / rojo" contesta *si* algo falló, pero no
responde las preguntas que realmente importan:

- ¿La suite está tardando cada vez más?
- ¿La latencia viene degradándose de a poco desde hace cinco builds?
- ¿Qué escenario es el que falla siempre y nadie arregla?
- ¿Estamos agregando pruebas al mismo ritmo que agregamos funcionalidades?

Esas preguntas son sobre **tendencia**, no sobre un build aislado. Por eso las
métricas se acumulan y se grafican: **un build informa; una serie de builds
enseña.**

---

## 2. Qué se mide y por qué

### 2.1 Métricas funcionales

| Métrica | Fuente | Para qué sirve | Umbral |
|---|---|---|---|
| Pruebas ejecutadas | `target/surefire-reports/TEST-*.xml` | Detecta pruebas borradas o desactivadas en silencio | No debe bajar |
| Pruebas fallidas | Surefire / Failsafe | Quality gate: rompe el build | 0 |
| Escenarios BDD ejecutados / fallidos | `cucumber-junit.xml` | Cobertura de reglas de negocio acordadas | 0 fallidos |
| Tasa de éxito | Calculada | Salud general de la suite | 100 % |
| Duración de la suite | Surefire | Si crece, el feedback se vuelve lento y el equipo deja de esperarlo | < 5 min |
| Pruebas inestables (*flaky*) | Comparación entre builds | Una prueba que falla y pasa sin cambios de código destruye la confianza en la suite | 0 |

### 2.2 Métricas de performance

| Indicador | Qué mide | Por qué se monitorea | Umbral (SLA) |
|---|---|---|---|
| **TPS** (transacciones por segundo) | Peticiones completadas por segundo | Es la capacidad real del sistema. Si cae con la misma carga, algo se degradó | > 5 TPS |
| **Latencia promedio** | Media de los tiempos de respuesta | Referencia general; **por sí sola engaña**: una media baja puede esconder una cola pésima | < 400 ms |
| **Latencia p95** | El 95 % de las peticiones responde en menos de este tiempo | Es el indicador contractual: describe la experiencia del 5 % peor atendido, que es quien reclama | < 800 ms |
| **Latencia p99** | Cola larga | Detecta pausas de GC, bloqueos de conexión, timeouts intermitentes | < 1500 ms |
| **Tasa de error** | % de respuestas con fallo técnico | Bajo carga aparecen errores que no se ven con un solo usuario | < 1 % |
| **Checks funcionales** | % de validaciones de negocio correctas bajo carga | Responder rápido pero mal no sirve | > 99 % |
| **VUs** (usuarios virtuales) | Concurrencia aplicada | Contextualiza todo lo anterior: 90 ms con 10 usuarios no es lo mismo que con 500 | 10 (carga esperada) |

> **Por qué el percentil 95 y no el promedio.** En la ejecución real de este
> proyecto el promedio fue **71,8 ms** y el p99 **331 ms**: casi 5 veces más. Si
> solo mirásemos el promedio, la cola larga —donde vive la mala experiencia—
> sería invisible.

---

## 3. Arquitectura: cómo llegan las métricas al dashboard

```
  Pipeline (GitHub Actions / Jenkins)
        │
        │  cada etapa emite un artefacto de datos
        ├── Surefire  ─► TEST-*.xml            (pruebas unitarias)
        ├── Cucumber  ─► cucumber.json / .xml  (escenarios BDD)
        └── k6        ─► indicadores.json      (TPS, latencia, errores)
                          │
                          ▼
             ┌─────────────────────────┐
             │  Paso "publicar métricas" │
             │  normaliza a un formato   │
             │  común (clave, valor,     │
             │  build, rama, fecha)      │
             └────────────┬────────────┘
                          │
        ┌─────────────────┼──────────────────┐
        ▼                 ▼                  ▼
  GITHUB_STEP_SUMMARY  GitHub Pages      Prometheus /
  (resumen del run)    (dashboard HTML)  InfluxDB ─► Grafana
   inmediato,           histórico del      histórico largo,
   dentro del run       proyecto           alertas y correlación
```

Se usan **tres niveles** porque responden a preguntas distintas:

1. **`$GITHUB_STEP_SUMMARY`** — tabla de resultados dentro del propio run. Es lo
   primero que ve quien abrió el Pull Request, sin descargar nada.
2. **GitHub Pages** — el dashboard HTML (`reportes/index.html`) publicado en una
   URL fija. Cualquiera del equipo lo abre sin permisos de CI ni conocimientos
   de Actions.
3. **Grafana sobre Prometheus/InfluxDB** — la serie histórica larga. Es donde se
   ven las tendencias de semanas y donde se configuran las alertas.

---

## 4. Implementación en el pipeline

### 4.1 Resumen inmediato en el run (ya implementado)

```yaml
- name: Publicar indicadores en el resumen del pipeline
  run: |
    echo "## Prueba de performance — POST /api/login" >> $GITHUB_STEP_SUMMARY
    node -e '
      const d = require("./performance/resultados/indicadores.json");
      console.log("| Indicador | Valor | Umbral | Resultado |");
      console.log("|---|---|---|---|");
      /* ... una fila por indicador ... */
    ' >> $GITHUB_STEP_SUMMARY
```

### 4.2 Publicación del dashboard en GitHub Pages (ya implementado)

El job `publicar-dashboard` reúne los artefactos de las tres etapas, los ordena
en un mismo sitio estático y lo despliega. El equipo entra siempre a la misma
URL y ve el último estado.

### 4.3 Envío a Grafana (simulado)

Con k6 basta cambiar la salida para que las métricas viajen a una base de series
de tiempo, sin tocar el script de la prueba:

```bash
# InfluxDB + Grafana
k6 run --out influxdb=http://influx:8086/k6 performance/login-carga.js

# Prometheus (remote write)
K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  k6 run --out experimental-prometheus-rw performance/login-carga.js
```

Para las métricas funcionales, un paso del pipeline convierte los XML a puntos
de la serie:

```bash
# Ejemplo de push de métricas funcionales al Pushgateway de Prometheus
cat <<EOF | curl --data-binary @- http://pushgateway:9091/metrics/job/ci/rama/$RAMA
pruebas_totales   $TOTAL
pruebas_fallidas  $FALLOS
escenarios_bdd    $ESCENARIOS
duracion_suite_ms $DURACION
EOF
```

En Jenkins el equivalente es el plugin **Performance** más `publishHTML`, ya
declarados en el `Jenkinsfile`.

---

## 5. Paneles del dashboard y la decisión que habilita cada uno

| Panel | Visualización | Decisión que permite tomar |
|---|---|---|
| Indicadores del último build | Tarjetas (stat tiles) | ¿Puedo desplegar ahora mismo? |
| Distribución de latencia por percentil | Barras horizontales con el umbral marcado | ¿Cuánta holgura tengo antes de incumplir el SLA? |
| Evolución del p95 por build | Línea + línea de umbral | ¿Se está degradando de a poco? ¿Qué commit lo rompió? |
| Tasa de éxito por build | Línea | ¿La suite es confiable o tenemos pruebas inestables? |
| Escenarios BDD por etiqueta | Barras | ¿Qué área de negocio concentra los fallos? |
| Vista de datos (tabla) | Tabla | Accesibilidad: la información nunca depende solo del color |

> **Criterio de diseño aplicado.** Cada gráfico compara el valor medido contra su
> umbral en el mismo panel: un número sin su umbral al lado no permite decidir
> nada. Los estados se señalan con **icono + texto** además del color, para que
> el dashboard siga siendo legible en impresión o para una persona con daltonismo.

---

## 6. Evidencia

- Dashboard implementado: [`reportes/index.html`](../reportes/index.html)
- Captura: `docs/evidencias/06-dashboard-metricas.png`
- Datos reales que alimentan el dashboard: `performance/resultados/indicadores.json`
