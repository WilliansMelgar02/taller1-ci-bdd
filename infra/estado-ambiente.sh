#!/usr/bin/env bash
# =============================================================================
#  Resume en Markdown el estado final del ambiente de pruebas.
#
#  Uso: infra/estado-ambiente.sh >> "$GITHUB_STEP_SUMMARY"
#
#  Deja trazabilidad de cada despliegue: qué versión quedó atendiendo, cuál
#  se descartó y si hubo rollback. Es la evidencia que se revisa sin tener que
#  leer los logs completos.
# =============================================================================
source "$(dirname "${BASH_SOURCE[0]}")/comun.sh"

ACTIVO="$(leer_estado color-activo)"
ROLLBACK="$(leer_estado rollback)"

echo "## Despliegue Blue-Green en staging"
echo
if [ -n "$ROLLBACK" ]; then
  echo "### ROLLBACK EJECUTADO"
  echo
  echo "> $(leer_estado resultado-rollback)"
elif [ "$ACTIVO" = "green" ]; then
  echo "### Versión candidata aprobada y con tráfico"
else
  echo "### El despliegue no llegó a completarse"
fi
echo
echo "| Color | Versión | Imagen | Tráfico |"
echo "|---|---|---|---|"
for color in blue green; do
  version="$(leer_estado "version-$color")"
  imagen="$(leer_estado "imagen-$color")"
  if [ "$ACTIVO" = "$color" ]; then trafico="**activo**"; else trafico="en espera"; fi
  if [ -z "$version" ]; then
    echo "| ${color} | — | — | sin desplegar |"
  else
    echo "| ${color} | \`${version}\` | \`${imagen}\` | ${trafico} |"
  fi
done
