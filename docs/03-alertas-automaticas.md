# Alertas automáticas ante fallos y degradaciones

> Un pipeline que falla en silencio no sirve de nada. Este documento describe qué
> se alerta, a quién, por qué canal y con qué urgencia.

---

## 1. Principio de diseño: la alerta debe ser accionable

La regla que ordena toda la estrategia:

> **Si una alerta no obliga a nadie a hacer algo, no debe existir.**

Alertar por todo produce *fatiga de alertas*: el equipo empieza a silenciar el
canal y el día que llega la alerta importante, nadie la mira. Por eso cada alerta
de este proyecto cumple cuatro condiciones:

1. **Tiene un dueño.** Alguien concreto es responsable de responder.
2. **Tiene un umbral objetivo.** No "va lento", sino "p95 > 800 ms".
3. **Trae contexto.** Rama, commit, autor, enlace directo al reporte.
4. **Tiene una acción esperada.** Revertir, corregir, investigar o ignorar de forma explícita.

---

## 2. Matriz de alertas

| # | Condición que la dispara | Severidad | Canal | Destinatario | Acción esperada |
|---|---|---|---|---|---|
| A1 | Falla una prueba unitaria | 🔴 Crítica | Check en el PR + Slack `#ci-alertas` | Autor del commit | Corregir antes de mezclar. El merge queda bloqueado |
| A2 | Falla un escenario BDD | 🔴 Crítica | Check en el PR + Slack | Autor + QA | Se rompió una regla de negocio acordada: corregir o renegociar el criterio |
| A3 | El pipeline falla en `main` | 🔴 Crítica | Slack `#ci-alertas` + correo + issue automático | Todo el equipo | `main` roto = nadie puede desplegar. Máxima prioridad |
| A4 | Latencia p95 > 800 ms | 🟠 Alta | Slack `#performance` | QA + Dev responsable | Investigar el commit que introdujo la degradación |
| A5 | Tasa de error > 1 % bajo carga | 🔴 Crítica | Slack + PagerDuty | Dev de turno | El sistema falla bajo carga normal: bloquear el despliegue |
| A6 | TPS cae > 20 % respecto al promedio de los 5 builds previos | 🟡 Media | Comentario en el PR | Autor del PR | Degradación gradual: revisar antes de que se acumule |
| A7 | Una prueba pasa a ser inestable (*flaky*) | 🟡 Media | Issue automático etiquetado `flaky` | QA | Estabilizar o cuarentena. Nunca ignorar |
| A8 | La duración de la suite crece > 30 % | 🟡 Media | Resumen semanal | Líder técnico | Optimizar o paralelizar antes de que el feedback se vuelva inútil |
| A9 | El build vuelve a verde tras un fallo | 🟢 Informativa | Slack `#ci-alertas` | Todo el equipo | Cerrar el incidente. Comunicar la recuperación también es parte de la alerta |

**Escalamiento:** una alerta crítica sin atender en 30 minutos escala al líder
técnico; a los 60 minutos, a la jefatura de desarrollo.

---

## 3. Implementación

### 3.1 El quality gate: la primera alerta es el build en rojo

La alerta más eficaz no es un mensaje, es **impedir que el código roto avance**.
Ya está implementado en tres capas:

| Capa | Mecanismo | Efecto |
|---|---|---|
| Maven | `testFailureIgnore=false` en Surefire | Una prueba roja detiene el build |
| k6 | `thresholds` en `options` | Si el SLA se incumple, k6 sale con código 99 y el paso falla |
| GitHub | *Branch protection rule* sobre `main` | Sin los checks en verde, el botón de merge queda deshabilitado |

### 3.2 Notificación a Slack (GitHub Actions)

```yaml
  alertas:
    name: 4 · Alertas automáticas
    runs-on: ubuntu-latest
    needs: [pruebas-unitarias, pruebas-bdd, performance]
    if: always()          # debe correr incluso cuando algo falló
    steps:
      - name: Notificar fallo a Slack
        if: contains(needs.*.result, 'failure')
        uses: slackapi/slack-github-action@v1
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
        with:
          payload: |
            {
              "text": ":rotating_light: *Pipeline FALLIDO* en `${{ github.ref_name }}`",
              "blocks": [
                { "type": "section", "text": { "type": "mrkdwn",
                  "text": "*Repositorio:* ${{ github.repository }}\n*Autor:* ${{ github.actor }}\n*Commit:* `${{ github.sha }}`" } },
                { "type": "section", "text": { "type": "mrkdwn",
                  "text": "*Unitarias:* ${{ needs.pruebas-unitarias.result }}\n*BDD:* ${{ needs.pruebas-bdd.result }}\n*Performance:* ${{ needs.performance.result }}" } },
                { "type": "actions", "elements": [
                  { "type": "button", "text": { "type": "plain_text", "text": "Ver el run" },
                    "url": "${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}" } ] }
              ]
            }
```

### 3.3 Issue automático cuando se rompe `main`

```yaml
      - name: Abrir incidente si main quedó roto
        if: failure() && github.ref == 'refs/heads/main'
        uses: actions/github-script@v7
        with:
          script: |
            await github.rest.issues.create({
              owner: context.repo.owner,
              repo: context.repo.repo,
              title: `[CI] main roto en el build #${context.runNumber}`,
              labels: ['ci', 'bloqueante'],
              body: `El pipeline falló en main.\n\n` +
                    `Commit: ${context.sha}\nAutor: ${context.actor}\n` +
                    `Run: ${context.serverUrl}/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId}`
            });
```

### 3.4 Alerta de degradación de performance (comparación entre builds)

No basta con comparar contra un umbral fijo: una degradación **gradual** nunca
cruza el umbral hasta que ya es tarde. Por eso se compara también contra la
línea base de los builds anteriores:

```bash
# Compara el p95 actual con la mediana de los 5 builds previos
P95_ACTUAL=$(node -p "require('./performance/resultados/indicadores.json').latencia_p95_ms")
P95_BASE=$(cat historico/p95-ultimos-5.txt | sort -n | awk '{a[NR]=$1} END {print a[int(NR/2)+1]}')
DEGRADACION=$(echo "scale=2; ($P95_ACTUAL - $P95_BASE) / $P95_BASE * 100" | bc)

if (( $(echo "$DEGRADACION > 20" | bc -l) )); then
  echo "::warning::Degradación de ${DEGRADACION}% en la latencia p95 respecto de la línea base"
  # dispara la alerta A6
fi
```

### 3.5 Alertas en Jenkins (ya implementado en el `Jenkinsfile`)

```groovy
post {
    failure  { mail to: 'equipo-qa@empresa.cl', subject: "[CI] FALLO en ${env.JOB_NAME} #${env.BUILD_NUMBER}", body: "..."
               slackSend channel: '#alertas-qa', color: 'danger',  message: ":rotating_light: Build #${env.BUILD_NUMBER} FALLÓ" }
    unstable { slackSend channel: '#alertas-qa', color: 'warning', message: ":warning: Build #${env.BUILD_NUMBER} INESTABLE" }
    fixed    { slackSend channel: '#alertas-qa', color: 'good',    message: ":white_check_mark: Build #${env.BUILD_NUMBER} recuperado" }
}
```

### 3.6 Alertas desde Grafana (monitoreo continuo, no solo en el pipeline)

```yaml
# Regla de alerta en Grafana sobre la serie de k6
- alert: LatenciaLoginDegradada
  expr: histogram_quantile(0.95, rate(k6_http_req_duration_bucket{endpoint="/api/login"}[5m])) > 0.8
  for: 5m                    # debe sostenerse 5 minutos: evita falsas alarmas por un pico aislado
  labels:   { severity: alta, equipo: qa }
  annotations:
    summary: "Latencia p95 del login sobre 800 ms"
    description: "p95 actual: {{ $value }}s. Revisar el último despliegue."
    runbook_url: "https://wiki.empresa.cl/runbooks/login-lento"
```

> El parámetro `for: 5m` es la diferencia entre una alerta útil y una molesta:
> exige que la condición **se sostenga** antes de avisar, filtrando los picos
> transitorios que se resuelven solos.

---

## 4. Simulación: cómo se ve una alerta real

Mensaje que llega a `#ci-alertas` cuando el build #5 cruzó el umbral de latencia
(escenario ilustrado en el dashboard):

```
🚨  Pipeline FALLIDO en main
    Repositorio: WilliansMelgar02/taller1-ci-bdd
    Autor:       WilliansMelgar02
    Commit:      3f9a1c2

    Unitarias:   ✅ success   (14/14)
    BDD:         ✅ success   (8/8)
    Performance: ❌ failure

    Umbral incumplido: http_req_duration p(95)=910ms  >  800ms
    Degradación: +920 % respecto de la línea base (89 ms)

    [ Ver el run ]  [ Ver el reporte de performance ]
```

Y el mensaje de recuperación (alerta A9) tras corregir:

```
✅  Build #6 recuperado en main — p95 volvió a 120 ms. Incidente cerrado.
```

---

## 5. Qué NO se alerta (y por qué)

| No se alerta | Motivo |
|---|---|
| Cada build exitoso | Ruido puro. El éxito es la norma esperada, no una noticia |
| Cambios de menos del 5 % en la latencia | Está dentro de la variación normal entre agentes de CI |
| Fallos en ramas `feature/**` de otras personas | Es responsabilidad de su autor; se avisa solo a quien hizo el commit |
| Advertencias del compilador | Se revisan en el análisis estático, no interrumpen a nadie |

---

## 6. Evidencia

- Alertas implementadas en el pipeline: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml), job `alertas`
- Alertas on-premise: [`Jenkinsfile`](../Jenkinsfile), bloque `post`
- Captura de la simulación: `docs/evidencias/07-alerta-simulada.png`
