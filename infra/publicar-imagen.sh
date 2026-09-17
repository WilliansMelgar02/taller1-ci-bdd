#!/usr/bin/env bash
# =============================================================================
#  Publica una imagen en el registro, con reintentos.
#
#  Uso: infra/publicar-imagen.sh <imagen>
#
#  El registro puede rechazar una subida por causas ajenas al código (por
#  ejemplo "unknown blob" cuando dos ejecuciones suben las mismas capas a la
#  vez). Un error transitorio no debe detener una entrega: se reintenta con
#  espera creciente, y solo si persiste el pipeline falla.
# =============================================================================
source "$(dirname "${BASH_SOURCE[0]}")/comun.sh"

IMAGEN="${1:?Falta la imagen a publicar, por ejemplo ghcr.io/usuario/portal-clientes:1.1.0-a1b2c3d}"
INTENTOS=3

for intento in $(seq 1 "$INTENTOS"); do
  if docker push --quiet "$IMAGEN"; then
    log "Imagen publicada: ${IMAGEN}"
    exit 0
  fi
  if [ "$intento" -lt "$INTENTOS" ]; then
    espera=$((intento * 15))
    advertir "No se pudo publicar ${IMAGEN} (intento ${intento}/${INTENTOS}); se reintenta en ${espera} s"
    sleep "$espera"
  fi
done

error "No se pudo publicar ${IMAGEN} tras ${INTENTOS} intentos"
exit 1
