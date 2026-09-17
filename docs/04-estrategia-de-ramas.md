# Estrategia de ramas: Trunk-Based Development

## 1. Decisión

El repositorio sigue **Trunk-Based Development**: una sola rama principal
(`main`) que siempre está en condiciones de desplegarse, y ramas de vida corta
que se integran mediante Pull Request en horas, no en semanas.

## 2. Por qué Trunk-Based y no GitFlow

El material del curso (ME_1, p. 8) entrega el criterio de elección:

> "GitFlow es una estrategia que define ramas para cada tipo de trabajo: develop,
> feature, release, hotfix. Es útil en equipos grandes donde hay ciclos largos y
> versiones estables planificadas."
>
> "Por otro lado, Trunk-Based Development propone trabajar directamente en una
> rama principal (main) con ramas muy cortas, ideal para equipos ágiles y
> despliegues continuos."

| Criterio | GitFlow | Trunk-Based | Este proyecto |
|---|---|---|---|
| Tamaño del equipo | Grande | Pequeño o ágil | Desarrollo individual |
| Ciclo de entrega | Largo y planificado | Continuo | Cada merge a `main` se despliega en staging y se promueve |
| Ramas de larga vida | `main`, `develop`, `release/*` | Solo `main` | Solo `main` |
| Riesgo de conflictos al integrar | Alto (ramas que divergen por semanas) | Bajo (integración diaria) | Ramas de 1 a 4 commits |
| Encaje con un deployment pipeline | Requiere decidir qué rama despliega a qué ambiente | Natural: `main` es la única fuente de verdad | `workflow_run` sobre `main` |

GitFlow agregaría ramas `develop` y `release/*` que no aportan valor cuando no
existen versiones paralelas que mantener: solo sumarían merges y conflictos. El
proyecto entrega de forma continua, que es exactamente el caso que el curso
asocia a Trunk-Based.

## 3. Reglas del flujo

```
main ●───────────●───────────●───────────●───────────●──────────►  siempre desplegable
      \         /  \         /  \         /  \         /
       ●───●───●    ●───●───●    ●───●───●    ●───●───●
     feature/api-http  feature/pruebas-  feature/despliegue-  fix/reintento-...
                       aceptacion        blue-green
```

1. **`main` siempre desplegable.** Todo lo que llega a `main` pasa por el CI y,
   a continuación, por el deployment pipeline, que lo promueve como versión
   estable.
2. **Ramas cortas con prefijo por intención:**

   | Prefijo | Uso | Ejemplo |
   |---|---|---|
   | `feature/` | Funcionalidad o capacidad nueva | `feature/despliegue-blue-green` |
   | `fix/` | Corrección de un defecto | `fix/reintento-publicacion-imagen` |
   | `chore/` | Mantenimiento sin cambio funcional | `chore/actualiza-github-actions` |
   | `style/` | Formato o presentación | `style/sin-emojis` |
   | `docs/` | Documentación | `docs/examen-final` |
   | `demo/` | Demostraciones que **nunca** se integran | `demo/rollback-defecto-interfaz` |

3. **Integración solo por Pull Request**, con el CI y el Acceptance Gate en
   verde. En cada PR corren el pipeline de CI y el deployment pipeline completo
   sobre un ambiente de staging efímero (sin promoción).
4. **Merge commit con mensaje uniforme** (`merge: integra ... (PR #n)`): deja en
   el historial un nodo por entrega, que documenta qué commits formaron parte de
   cada integración.
5. **Commits atómicos con Conventional Commits** (`tipo(alcance): resumen`),
   cuyo cuerpo explica el porqué del cambio.
6. **Versionado en tres niveles:**
   - versión semántica en el `pom.xml` (`1.1.0`);
   - imagen Docker inmutable por commit (`portal-clientes:1.1.0-4378134`);
   - tag y release de Git por versión promovida (`v1.1.0`).

## 4. Historial real de integraciones

| PR | Rama | Qué integró | Resultado |
|---|---|---|---|
| #1 | `feature/api-http` | API HTTP, interfaz web y pruebas de integración | Integrado |
| #2 | `feature/pruebas-aceptacion` | Pruebas de aceptación con Selenium y perfil Maven | Integrado |
| #3 | `feature/despliegue-blue-green` | Dockerfile, scripts Blue-Green, deployment pipeline y Jenkinsfile | Integrado |
| #4 | `feature/performance-app-real` | k6 mide el jar real en lugar de un mock | Integrado |
| #5 | `feature/analisis-estatico` | SpotBugs y Find Security Bugs en el commit stage | Integrado |
| #6 | `feature/rollback-manual` | Workflow de rollback manual asistido | Integrado |
| #7 | `fix/reintento-publicacion-imagen` | Reintentos ante errores transitorios del registro | Integrado |
| #8 | `demo/rollback-defecto-interfaz` | Defecto intencional para demostrar el rollback automático | **Cerrado sin integrar** |
| #9 | `chore/actualiza-github-actions` | GitHub Actions en versiones con Node 24 | Integrado |
| #10 | `style/sin-emojis` | Estados en texto en pipelines y alertas | Integrado |

Cada integración se hizo de a una y solo después de verificar que el CI y el
deployment pipeline de `main` quedaban en verde con la anterior.

## 5. Protección de la rama `main`

El material pide "Pull requests protegidos: requerir code review y aprobación
antes de ejecutar stages de deploy" (ME_6, p. 14). La regla de protección
recomendada para `main` es:

| Opción (Settings → Branches) | Valor | Motivo |
|---|---|---|
| Require a pull request before merging | Sí | Nadie integra directo a `main` |
| Require status checks to pass | Sí | Checks obligatorios del CI y del Acceptance Gate |
| Checks requeridos | `1 · Compilación, análisis estático y unitarias`, `2 · Integración y escenarios BDD`, `3 · Prueba de performance (k6)`, `2 · Blue-Green en staging + Acceptance Gate` | Un cambio que rompe cualquier nivel de prueba no se puede integrar |
| Require branches to be up to date | Sí | El PR se prueba contra el `main` actual |
| Do not allow bypassing | Sí | La regla aplica también a administradores |
| Allow force pushes / deletions | No | El historial de `main` es inmutable |
