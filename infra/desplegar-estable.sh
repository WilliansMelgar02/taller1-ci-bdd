#!/usr/bin/env bash
# =============================================================================
#  Levanta en BLUE la última versión aprobada y le entrega el tráfico.
#
#  Uso: infra/desplegar-estable.sh <imagen-estable>
#
#  Representa lo que hoy usan los clientes. Es el punto de retorno del
#  rollback: si la versión candidata falla, el tráfico vuelve aquí.
# =============================================================================
source "$(dirname "${BASH_SOURCE[0]}")/comun.sh"

IMAGEN="${1:?Falta la imagen estable, por ejemplo ghcr.io/usuario/portal-clientes:estable}"

levantar_color blue "$IMAGEN"
"$(dirname "${BASH_SOURCE[0]}")/cambiar-trafico.sh" blue
