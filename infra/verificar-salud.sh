#!/usr/bin/env bash
# =============================================================================
#  Health check con verificación de versión.
#
#  Uso: infra/verificar-salud.sh <url-base> <version-esperada> [intentos]
#
#  No basta con que la aplicación responda: tiene que responder la versión
#  que se acaba de desplegar. Un proxy que sigue apuntando a la versión
#  anterior también contesta 200, y sin esta verificación el pipeline daría
#  por bueno un despliegue que nunca ocurrió.
# =============================================================================
source "$(dirname "${BASH_SOURCE[0]}")/comun.sh"

URL_BASE="${1:?Falta la URL base, por ejemplo http://localhost:8082}"
VERSION_ESPERADA="${2:?Falta la versión esperada}"
INTENTOS="${3:-30}"
PAUSA_SEGUNDOS=2

for intento in $(seq 1 "$INTENTOS"); do
  respuesta="$(curl -sf --max-time 3 "${URL_BASE}/health" || true)"
  version="$(printf '%s' "$respuesta" | sed -n 's/.*"version":"\([^"]*\)".*/\1/p')"

  if [ -n "$respuesta" ] && [ "$version" = "$VERSION_ESPERADA" ]; then
    log "Sano: ${URL_BASE} responde la versión ${version} (intento ${intento}/${INTENTOS})"
    log "  ${respuesta}"
    exit 0
  fi

  if [ -n "$version" ]; then
    advertir "${URL_BASE} responde la versión '${version}', se esperaba '${VERSION_ESPERADA}' (intento ${intento}/${INTENTOS})"
  fi
  sleep "$PAUSA_SEGUNDOS"
done

error "${URL_BASE} no respondió la versión ${VERSION_ESPERADA} tras ${INTENTOS} intentos"
exit 1
