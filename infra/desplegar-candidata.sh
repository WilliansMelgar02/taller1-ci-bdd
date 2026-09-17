#!/usr/bin/env bash
# =============================================================================
#  Despliega la versión candidata en GREEN, sin entregarle tráfico.
#
#  Uso: infra/desplegar-candidata.sh <imagen-candidata>
#
#  GREEN queda accesible solo por su propio puerto (8082) para que el
#  Acceptance Gate la pruebe mientras los clientes siguen en BLUE.
# =============================================================================
source "$(dirname "${BASH_SOURCE[0]}")/comun.sh"

IMAGEN="${1:?Falta la imagen candidata, por ejemplo ghcr.io/usuario/portal-clientes:1.1.0-a1b2c3d}"

# Se registra antes de levantarla: si el arranque falla, el rollback igual
# sabe qué color descartar.
guardar_estado "color-candidato" "green"
levantar_color green "$IMAGEN"
log "Versión candidata lista en http://localhost:$(puerto_de green) (todavía sin tráfico)"
