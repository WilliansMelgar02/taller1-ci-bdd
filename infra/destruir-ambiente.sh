#!/usr/bin/env bash
# =============================================================================
#  Destruye el ambiente de pruebas efímero.
#
#  Uso: infra/destruir-ambiente.sh
#
#  Se ejecuta siempre al final del pipeline, haya pasado o fallado. En un
#  agente compartido (Jenkins), un contenedor olvidado ocupa los puertos y
#  hace fallar la siguiente ejecución por una razón ajena al código.
# =============================================================================
source "$(dirname "${BASH_SOURCE[0]}")/comun.sh"

log "Destruyendo el ambiente de pruebas"
docker rm -f "$CONTENEDOR_PROXY" "$(contenedor_de blue)" "$(contenedor_de green)" >/dev/null 2>&1 || true
docker network rm "$RED_STAGING" >/dev/null 2>&1 || true
log "Ambiente destruido (el estado y los logs quedan en ${DIRECTORIO_ESTADO})"
