# Deployment pipeline: Blue-Green con rollback automático

## 1. Qué es y qué resuelve

> "Un deployment pipeline es la representación automatizada y codificada de todo
> el proceso que transforma un cambio de código en un artefacto desplegable y lo
> entrega a entornos de prueba o producción." (ME_5, p. 6)

En este proyecto el deployment pipeline continúa donde termina el pipeline de
integración continua: toma un commit que ya pasó el análisis estático, las
pruebas unitarias, de integración, BDD y de performance, lo empaqueta como
imagen Docker, lo despliega en un ambiente de pruebas con la estrategia
**Blue-Green**, ejecuta las **pruebas de aceptación** contra la versión
desplegada y, solo si todo pasa, le entrega el tráfico y la promueve como
versión estable. Si algo falla, un **rollback automático** devuelve el tráfico a
la versión anterior.

| Archivo | Rol |
|---|---|
| `.github/workflows/despliegue.yml` | Deployment pipeline (GitHub Actions) |
| `.github/workflows/rollback-manual.yml` | Rollback manual asistido |
| `Jenkinsfile` | Equivalente on-premise: CI + despliegue, con `post { failure }` |
| `Dockerfile` | Imagen de ejecución del portal |
| `infra/*.sh` | Acciones de despliegue, compartidas por GitHub Actions y Jenkins |

## 2. Etapas

La secuencia sigue la que propone el curso: *Git Push → Build → Static Analysis
→ Unit Tests → Package Artefact → Deploy to Staging → Acceptance Gate → Deploy
to Production* (ME_5, Imagen 2).

```
  push a main / Pull Request
          │
          ▼
  ┌─────────────────────────── CI (ci.yml) ────────────────────────────┐
  │ compilar → análisis estático → unitarias → integración + BDD → k6  │
  └─────────────────────────────────┬──────────────────────────────────┘
                                    │ solo si terminó en verde
          ┌─────────────────────────▼──────────────────────────────────┐
  1 ·     │ Empaquetar: jar + imagen Docker 1.1.0-<commit> → GHCR      │
          └─────────────────────────┬──────────────────────────────────┘
          ┌─────────────────────────▼──────────────────────────────────┐
  2 ·     │ Staging efímero (red Docker + proxy NGINX :8080)           │
          │   a. versión estable     → BLUE  (:8081) + tráfico         │
          │   b. versión candidata   → GREEN (:8082) sin tráfico       │
          │   c. health check con verificación de versión              │
          │   d. Acceptance Gate: Selenium + API contra GREEN          │
          │   e. cambio de tráfico a GREEN                             │
          │   f. smoke test a través del proxy                         │
          │   si algo falla → infra/rollback.sh (tráfico vuelve a BLUE)│
          └─────────────────────────┬──────────────────────────────────┘
          ┌─────────────────────────▼──────────────────────────────────┐
  3 ·     │ Promover (solo main): etiqueta 'estable' + release vX.Y.Z  │
          └────────────────────────────────────────────────────────────┘
```

| Etapa | Qué hace | Por qué así |
|---|---|---|
| Disparo | `workflow_run` cuando el CI de `main` termina en verde; también en cada PR y manualmente | No se empaqueta un commit que no pasó las pruebas |
| 1 · Empaquetar | `mvn package`, `docker build` con etiquetas OCI (versión, commit, fecha) y publicación en GitHub Container Registry | *Build once, deploy many*: la imagen que se prueba es la misma que se promueve |
| 2a · BLUE | Levanta la imagen `estable` y le entrega el tráfico | Representa lo que usan hoy los clientes y es el punto de retorno del rollback |
| 2b · GREEN | Levanta la imagen candidata sin tráfico | Se valida sin exponer a los clientes |
| 2c · Health check | `/health` debe responder la **versión exacta** que se desplegó | Un proxy que sigue apuntando a la versión anterior también responde 200 |
| 2d · Acceptance Gate | 8 escenarios de aceptación: 4 con Selenium en el navegador y 4 contra la API | "Acceptance Test: Pruebas de regresión contra escenarios críticos" (ME_3, p. 7) |
| 2e · Cambio de tráfico | Reescribe el upstream de NGINX y recarga sin cortar conexiones | Switch instantáneo sin downtime |
| 2f · Smoke test | Verifica por el proxy que responde la versión nueva | Confirma que el switch realmente ocurrió |
| 3 · Promover | Etiqueta la imagen como `estable` y `1.1.0`, y crea la release de Git | La próxima entrega usará esta versión como BLUE |

## 3. El ambiente de pruebas

El ambiente de staging es **efímero**: se crea desde cero en el agente del
pipeline, se usa y se destruye al terminar. El curso lo recomienda de forma
explícita:

> "Entornos efímeros y reproductibles. Ejecuta cada stage en un entorno limpio
> (contenedor Docker o máquina virtual recién provisionada)." (ME_5, p. 9)

| Componente | Imagen | Puerto |
|---|---|---|
| Proxy (router Blue-Green) | `nginx:1.27-alpine` | 8080 |
| Portal BLUE (versión estable) | `ghcr.io/williansmelgar02/portal-clientes:estable` | 8081 |
| Portal GREEN (versión candidata) | `ghcr.io/williansmelgar02/portal-clientes:<versión>-<commit>` | 8082 |

Ventajas de este diseño:

- **Idéntico en cada ejecución.** Nadie modifica el ambiente a mano, así que no
  acumula configuración olvidada.
- **Misma imagen en todos lados.** Lo que distingue a BLUE de GREEN es solo la
  variable `COLOR_DESPLIEGUE`; el artefacto es el mismo que se promueve.
- **Sin costo ni credenciales externas.** Todo corre en el agente de CI, sin
  cuentas de nube.

## 4. Por qué Blue-Green y no Canary

El material pide justificar la estrategia considerando "tipo de aplicación,
recursos disponibles, impacto del fallo y experiencia del equipo" (ME_6, p. 11).

| Criterio | Blue-Green | Canary | Decisión |
|---|---|---|---|
| Tipo de aplicación | Una instancia por versión; conmutación total | Varias réplicas y reparto porcentual del tráfico | El portal es un servicio único sin réplicas: Blue-Green encaja |
| Recursos disponibles | Dos contenedores ("coste duplicado de recursos", ME_6 p. 8) | Router con pesos e infraestructura de métricas en tiempo real | Dos contenedores en el agente de CI no tienen costo adicional |
| Impacto del fallo | La candidata se valida **antes** de recibir tráfico | Un porcentaje de usuarios reales ve el fallo | Con Blue-Green ningún cliente ve una versión defectuosa |
| Validación posible | Pruebas de aceptación completas contra GREEN | Requiere tráfico real para medir | Un ambiente efímero no tiene usuarios reales: un canary no mediría nada |
| Experiencia y simplicidad | Un switch de proxy y un rollback trivial | "Manejo más complejo del router o Ingress controller" (ME_6 p. 9) | Menos piezas, menos puntos de falla |

## 5. Rollback

### 5.1 Rollback automático

Es el equivalente en GitHub Actions al patrón del curso:

> "Si cualquier paso falla (tests en staging o deploy), el bloque post.failure
> ejecuta el script de rollback." (ME_6, p. 6)

```yaml
- name: Rollback automático a la versión estable
  if: failure() && steps.candidata.outcome != 'skipped'
  run: ./infra/rollback.sh
```

`infra/rollback.sh` cubre los dos momentos posibles del fallo:

| Momento del fallo | Estado del tráfico | Acción del rollback |
|---|---|---|
| Antes del switch (health check o Acceptance Gate) | Sigue en BLUE | Confirma por el proxy que responde la versión estable y descarta GREEN |
| Después del switch (smoke test) | Ya está en GREEN | Devuelve el tráfico a BLUE, lo verifica y descarta GREEN |
| Primera entrega (no hay BLUE) | Sin versión previa | Deja el proxy en 503 en lugar de servir una versión rota |

En todos los casos:
- **Guarda evidencia:** los logs de la candidata en `logs-candidata.txt` y los metadatos en `deploy-metadata.json`.
- **Verifica el resultado:** un rollback que no comprueba que el tráfico volvió no es un rollback, es una esperanza.

### 5.2 Rollback manual asistido

Sirve para cuando un defecto aparece **después** de promover una versión.
Desde *Actions → Rollback manual asistido* se indica la versión y el motivo, y
el workflow:

1. valida que la imagen exista y que su commit pertenezca a `main`;
2. la levanta en GREEN mientras la versión estable actual sigue en BLUE;
3. ejecuta **las pruebas de aceptación de esa versión** (con `git worktree`),
   porque la suite actual podría exigir funcionalidades que la versión antigua
   no tenía;
4. cambia el tráfico y mueve la etiqueta `estable`;
5. si la versión elegida tampoco es apta, ejecuta `infra/rollback.sh`;
6. registra quién lo pidió y por qué en `rollback-metadata.json`.

### 5.3 Versiones preservadas

> "Mantén versiones previas de artefactos y scripts de despliegue que permitan
> revertir rápidamente en caso de fallo en producción." (ME_5, p. 9)

- **Imágenes inmutables.** Cada imagen queda en el registro con una etiqueta que no se reutiliza (`1.1.0-<commit>`).
- **`latest` no se usa.** Con una etiqueta que se mueve no hay una versión concreta a la cual volver.
- **Scripts en el repositorio.** Los scripts de despliegue y rollback están versionados en `infra/`, junto al código.

## 6. Scripts de `infra/`

| Script | Responsabilidad |
|---|---|
| `comun.sh` | Configuración compartida: red, puertos, archivos de estado, generación de la configuración de NGINX |
| `preparar-ambiente.sh` | Crea la red Docker y el proxy (responde 503 hasta que haya versión activa) |
| `desplegar-estable.sh` | Levanta la versión estable en BLUE y le entrega el tráfico |
| `desplegar-candidata.sh` | Levanta la versión candidata en GREEN, sin tráfico |
| `verificar-salud.sh` | Health check con reintentos que exige la versión esperada |
| `cambiar-trafico.sh` | Switch del proxy a un color, con verificación |
| `rollback.sh` | Rollback automático verificado |
| `publicar-imagen.sh` | Publica en el registro con reintentos ante errores transitorios |
| `estado-ambiente.sh` | Resumen del despliegue en Markdown |
| `destruir-ambiente.sh` | Elimina contenedores y red al terminar |

Como la lógica vive en estos scripts y no en la herramienta de CI, el
`Jenkinsfile` ejecuta exactamente los mismos pasos que GitHub Actions.

## 7. Trazabilidad y auditoría

- **Imagen:** cada imagen lleva etiquetas OCI con versión, commit y fecha de construcción.
- **Página del portal y `/health`:** informan versión, commit y color. Es la prueba visible de hacia dónde apunta el tráfico.
- **Metadatos del despliegue:** cada ejecución publica `deploy-metadata.json` con pipeline, ejecución, evento, rama, commit, actor, imagen, resultado, si hubo rollback y el color final.
- **Grupo de concurrencia `despliegue-main`:** lo comparten los despliegues de `main` y los rollbacks manuales, así nunca modifican la versión estable al mismo tiempo.

## 8. Evidencias de ejecución

| Evidencia | Ejecución | Resultado |
|---|---|---|
| Despliegue Blue-Green en `main` | [run 35172696225](https://github.com/WilliansMelgar02/taller1-ci-bdd/actions/runs/35172696225) | Empaquetado, Acceptance Gate 8/8, switch y promoción |
| Rollback automático (PR #8, defecto intencional) | [run 35171569525](https://github.com/WilliansMelgar02/taller1-ci-bdd/actions/runs/35171569525) | Acceptance Gate 6/8, rollback ejecutado, tráfico restaurado en BLUE |
| Rollback manual asistido a `1.1.0-4c77e2c` | [run 35171829540](https://github.com/WilliansMelgar02/taller1-ci-bdd/actions/runs/35171829540) | Versión restaurada, validada y marcada como estable |

Los logs completos de esas ejecuciones están en
[`docs/evidencias/logs/`](evidencias/logs/) y los metadatos de auditoría en
[`docs/evidencias/auditoria/`](evidencias/auditoria/).
