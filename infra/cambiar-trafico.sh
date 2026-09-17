#!/usr/bin/env bash
# =============================================================================
#  Cambia el tráfico del proxy al color indicado (el "switch" de Blue-Green).
#
#  Uso: infra/cambiar-trafico.sh <blue|green>
#
#  El cambio es instantáneo y reversible: NGINX recarga su configuración sin
#  cortar conexiones, y el color anterior sigue corriendo, listo para volver.
# =============================================================================
source "$(dirname "${BASH_SOURCE[0]}")/comun.sh"

COLOR="${1:?Falta el color destino (blue o green)}"
validar_color "$COLOR"

if ! contenedor_en_ejecucion "$(contenedor_de "$COLOR")"; then
  error "No se puede enviar tráfico a ${COLOR}: su contenedor no está en ejecución"
  exit 1
fi

ACTIVO_ACTUAL="$(leer_estado color-activo)"
VERSION_DESTINO="$(leer_estado "version-$COLOR")"

log "Cambiando el tráfico: ${ACTIVO_ACTUAL:-ninguno} → ${COLOR} (versión ${VERSION_DESTINO})"

# El estado se registra ANTES de verificar: si la verificación falla, el
# rollback necesita saber cuál era el color anterior para volver a él.
guardar_estado "color-anterior" "$ACTIVO_ACTUAL"
guardar_estado "color-activo" "$COLOR"

escribir_configuracion_proxy "$COLOR"
recargar_proxy

"$(dirname "${BASH_SOURCE[0]}")/verificar-salud.sh" "http://localhost:${PUERTO_PROXY}" "$VERSION_DESTINO" 10
log "El proxy entrega tráfico a ${COLOR}"
