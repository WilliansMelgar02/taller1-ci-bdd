#!/usr/bin/env bash
# =============================================================================
#  ROLLBACK AUTOMÁTICO del despliegue Blue-Green.
#
#  Uso: infra/rollback.sh
#
#  El pipeline lo ejecuta cuando falla cualquier paso posterior al despliegue
#  de la candidata (health check, Acceptance Gate, cambio de tráfico o smoke
#  test). Sirve para ambos momentos del fallo:
#
#   - Antes del switch: el tráfico nunca salió de BLUE; basta con confirmarlo
#     y descartar GREEN.
#   - Después del switch: el tráfico ya está en GREEN; se devuelve a BLUE.
#
#  En los dos casos se conservan los logs de la candidata como evidencia del
#  fallo, y el rollback termina verificando que el proxy responde la versión
#  estable. Un rollback que no se verifica es solo una esperanza.
# =============================================================================
source "$(dirname "${BASH_SOURCE[0]}")/comun.sh"

CANDIDATO="$(leer_estado color-candidato)"
CANDIDATO="${CANDIDATO:-green}"
if [ "$CANDIDATO" = "green" ]; then ESTABLE="blue"; else ESTABLE="green"; fi

CONTENEDOR_CANDIDATO="$(contenedor_de "$CANDIDATO")"
CONTENEDOR_ESTABLE="$(contenedor_de "$ESTABLE")"
VERSION_CANDIDATA="$(leer_estado "version-$CANDIDATO")"
VERSION_ESTABLE="$(leer_estado "version-$ESTABLE")"

log "════════════════ ROLLBACK ════════════════"
log "Versión candidata descartada: ${VERSION_CANDIDATA:-desconocida} (${CANDIDATO})"
log "Tráfico activo al iniciar el rollback: $(leer_estado color-activo || true)"
guardar_estado "rollback" "ejecutado"

# 1. Conservar la evidencia antes de eliminar nada.
if docker inspect "$CONTENEDOR_CANDIDATO" >/dev/null 2>&1; then
  docker logs "$CONTENEDOR_CANDIDATO" > "$DIRECTORIO_ESTADO/logs-candidata.txt" 2>&1 || true
  log "Logs de la candidata guardados en ${DIRECTORIO_ESTADO}/logs-candidata.txt"
fi

# 2. Devolver el tráfico a la versión estable, si existe.
if contenedor_en_ejecucion "$CONTENEDOR_ESTABLE"; then
  if [ "$(leer_estado color-activo)" != "$ESTABLE" ]; then
    "$(dirname "${BASH_SOURCE[0]}")/cambiar-trafico.sh" "$ESTABLE"
  else
    log "El tráfico nunca salió de ${ESTABLE}: los clientes no vieron la versión candidata"
    "$(dirname "${BASH_SOURCE[0]}")/verificar-salud.sh" "http://localhost:${PUERTO_PROXY}" "$VERSION_ESTABLE" 10
  fi
  RESULTADO="Tráfico restaurado en ${ESTABLE} con la versión ${VERSION_ESTABLE}"
else
  # Primera entrega: no hay versión anterior a la cual volver. Se deja el
  # proxy en 503 en lugar de seguir enviando tráfico a una versión rota.
  advertir "No hay versión estable en ${ESTABLE}: el ambiente queda sin versión activa"
  guardar_estado "color-activo" ""
  escribir_configuracion_proxy ""
  recargar_proxy
  RESULTADO="Sin versión estable previa: el ambiente queda en mantenimiento (503)"
fi

# 3. Descartar la candidata fallida.
docker rm -f "$CONTENEDOR_CANDIDATO" >/dev/null 2>&1 || true
guardar_estado "resultado-rollback" "$RESULTADO"

log "Candidata ${CANDIDATO} eliminada"
log "Resultado: ${RESULTADO}"
log "══════════════════════════════════════════"
