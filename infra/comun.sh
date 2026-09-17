#!/usr/bin/env bash
# =============================================================================
#  Configuración y funciones compartidas del ambiente de pruebas (staging).
#
#  El ambiente sigue la estrategia BLUE-GREEN:
#
#                        ┌──────────────► portal-blue   (versión estable)
#     :8080 ──► proxy ───┤
#                        └ ─ ─ ─ ─ ─ ─ ─► portal-green  (versión candidata)
#
#  - BLUE atiende el tráfico con la última versión aprobada.
#  - GREEN recibe la versión nueva, que se valida por su propio puerto (:8082)
#    SIN tráfico real: health check y Acceptance Gate.
#  - Solo si GREEN pasa todo, el proxy cambia el tráfico a GREEN y BLUE queda
#    en espera. Si algo falla, rollback.sh devuelve el tráfico a BLUE.
#
#  Lo usan por igual el workflow de GitHub Actions y el Jenkinsfile: la lógica
#  del despliegue vive en estos scripts versionados, no en la herramienta de CI.
# =============================================================================
set -euo pipefail

RED_STAGING="${RED_STAGING:-staging-red}"
CONTENEDOR_PROXY="${CONTENEDOR_PROXY:-staging-proxy}"
IMAGEN_PROXY="${IMAGEN_PROXY:-nginx:1.27-alpine}"
PUERTO_PROXY="${PUERTO_PROXY:-8080}"

# El estado del ambiente (color activo, versiones, logs) se guarda en archivos
# para que cada paso del pipeline, y el rollback, sepan en qué punto quedó.
DIRECTORIO_ESTADO="${DIRECTORIO_ESTADO:-$PWD/.staging}"

log() {
  printf '\033[1;34m[staging]\033[0m %s\n' "$*"
}

advertir() {
  printf '\033[1;33m[staging] ATENCIÓN:\033[0m %s\n' "$*"
}

error() {
  printf '\033[1;31m[staging] ERROR:\033[0m %s\n' "$*" >&2
}

validar_color() {
  case "$1" in
    blue|green) ;;
    *) error "Color desconocido: '$1' (se esperaba blue o green)"; exit 2 ;;
  esac
}

puerto_de() {
  validar_color "$1"
  if [ "$1" = "blue" ]; then echo 8081; else echo 8082; fi
}

contenedor_de() {
  validar_color "$1"
  echo "portal-$1"
}

guardar_estado() {
  mkdir -p "$DIRECTORIO_ESTADO"
  printf '%s' "$2" > "$DIRECTORIO_ESTADO/$1"
}

leer_estado() {
  cat "$DIRECTORIO_ESTADO/$1" 2>/dev/null || true
}

contenedor_en_ejecucion() {
  [ "$(docker inspect -f '{{.State.Running}}' "$1" 2>/dev/null || true)" = "true" ]
}

# Versión que declara la propia imagen en su etiqueta OCI. Se lee de la imagen,
# y no de un parámetro, para que sea imposible verificar la versión equivocada.
version_de_imagen() {
  docker image inspect -f '{{ index .Config.Labels "org.opencontainers.image.version" }}' "$1"
}

# Genera la configuración de NGINX. Sin color, el proxy responde 503: es
# preferible un "no disponible" explícito a enviar tráfico a una versión rota.
escribir_configuracion_proxy() {
  local color="${1:-}"
  local archivo="$DIRECTORIO_ESTADO/nginx/default.conf"
  mkdir -p "$DIRECTORIO_ESTADO/nginx"

  if [ -z "$color" ]; then
    cat > "$archivo" <<'NGINX'
# Sin versión activa: el ambiente no enruta tráfico.
server {
  listen 80;
  location / {
    default_type application/json;
    return 503 '{"mensaje":"Sin version activa en staging"}';
  }
}
NGINX
  else
    cat > "$archivo" <<NGINX
# Generado por infra/cambiar-trafico.sh — el tráfico apunta a ${color}.
upstream portal_activo {
  server $(contenedor_de "$color"):8080;
}
server {
  listen 80;
  location / {
    proxy_pass http://portal_activo;
    proxy_set_header Host \$host;
    proxy_set_header X-Real-IP \$remote_addr;
    add_header X-Color-Activo ${color} always;
  }
}
NGINX
  fi
}

recargar_proxy() {
  docker exec "$CONTENEDOR_PROXY" nginx -t -q
  docker exec "$CONTENEDOR_PROXY" nginx -s reload
}

# Levanta la imagen en el color indicado y espera a que responda sana.
levantar_color() {
  local color="$1" imagen="$2"
  local contenedor puerto version
  contenedor="$(contenedor_de "$color")"
  puerto="$(puerto_de "$color")"

  # Cada etapa del pipeline corre en un agente limpio: la imagen se descarga
  # del registro, que es la única fuente del artefacto aprobado.
  docker image inspect "$imagen" >/dev/null 2>&1 || docker pull --quiet "$imagen" >/dev/null
  version="$(version_de_imagen "$imagen")"

  log "Levantando ${imagen} en ${color} (versión ${version}, puerto ${puerto})"
  docker rm -f "$contenedor" >/dev/null 2>&1 || true
  docker run -d \
    --name "$contenedor" \
    --network "$RED_STAGING" \
    --publish "${puerto}:8080" \
    --env COLOR_DESPLIEGUE="$color" \
    --label ambiente=staging \
    --label color="$color" \
    "$imagen" >/dev/null

  guardar_estado "imagen-$color" "$imagen"
  guardar_estado "version-$color" "$version"

  "$(dirname "${BASH_SOURCE[0]}")/verificar-salud.sh" "http://localhost:${puerto}" "$version"
}
