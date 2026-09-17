#!/usr/bin/env bash
# =============================================================================
#  Aprovisiona el ambiente de pruebas efímero: red Docker y proxy NGINX.
#
#  Uso: infra/preparar-ambiente.sh
#
#  El ambiente se crea desde cero en cada ejecución del pipeline y se destruye
#  al final (destruir-ambiente.sh): nadie lo modifica a mano, así que siempre
#  es reproducible.
# =============================================================================
source "$(dirname "${BASH_SOURCE[0]}")/comun.sh"

log "Preparando el ambiente de pruebas en ${DIRECTORIO_ESTADO}"
rm -rf "$DIRECTORIO_ESTADO"
mkdir -p "$DIRECTORIO_ESTADO"

if ! docker network inspect "$RED_STAGING" >/dev/null 2>&1; then
  docker network create "$RED_STAGING" >/dev/null
  log "Red ${RED_STAGING} creada"
fi

# Al inicio no hay versión activa: el proxy responde 503 hasta que se active un color.
escribir_configuracion_proxy ""

docker rm -f "$CONTENEDOR_PROXY" >/dev/null 2>&1 || true
docker run -d \
  --name "$CONTENEDOR_PROXY" \
  --network "$RED_STAGING" \
  --publish "${PUERTO_PROXY}:80" \
  --volume "$DIRECTORIO_ESTADO/nginx:/etc/nginx/conf.d:ro" \
  --label ambiente=staging \
  "$IMAGEN_PROXY" >/dev/null

guardar_estado "color-activo" ""
log "Proxy ${CONTENEDOR_PROXY} escuchando en http://localhost:${PUERTO_PROXY} (sin versión activa)"
