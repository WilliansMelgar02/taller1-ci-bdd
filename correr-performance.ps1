# ---------------------------------------------------------------------------
#  Ejecuta la prueba de carga completa: levanta el servicio bajo prueba,
#  corre k6 contra él y lo detiene al terminar.
#
#  Uso:   .\correr-performance.ps1
#
#  Es el mismo flujo que ejecuta la etapa 3 del pipeline de CI.
# ---------------------------------------------------------------------------

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

Write-Host ""
# La carpeta de resultados esta en .gitignore porque son archivos generados,
# de modo que en un clon recien hecho no existe. k6 no crea directorios: si
# falta, la prueba corre pero no puede escribir sus reportes.
New-Item -ItemType Directory -Path (Join-Path $PSScriptRoot 'performance/resultados') -Force | Out-Null

Write-Host "  [1/3] Levantando el servicio de login en http://localhost:8088" -ForegroundColor Cyan
$servidor = Start-Process node -ArgumentList "performance\servidor-mock.js" -PassThru -WindowStyle Hidden

# Esperar a que el servicio responda antes de empezar a medir: si k6 arranca
# antes de que el puerto este escuchando, los primeros errores contaminan la
# tasa de error y la prueba falla por una razon que no es de rendimiento.
$arriba = $false
foreach ($intento in 1..20) {
    try {
        Invoke-RestMethod -Uri "http://localhost:8088/health" -TimeoutSec 2 | Out-Null
        $arriba = $true
        Write-Host "        Servicio arriba tras $intento intento(s)" -ForegroundColor Green
        break
    } catch { Start-Sleep -Seconds 1 }
}

if (-not $arriba) {
    Write-Host "  El servicio no respondio a tiempo." -ForegroundColor Red
    Stop-Process -Id $servidor.Id -Force -ErrorAction SilentlyContinue
    exit 1
}

Write-Host ""
Write-Host "  [2/3] Ejecutando la prueba de carga (55 segundos)" -ForegroundColor Cyan
Write-Host ""

k6 run performance\login-carga.js
$resultado = $LASTEXITCODE

Write-Host ""
Write-Host "  [3/3] Deteniendo el servicio" -ForegroundColor Cyan
Stop-Process -Id $servidor.Id -Force -ErrorAction SilentlyContinue

Write-Host ""
if ($resultado -eq 0) {
    Write-Host "  Prueba APROBADA: todos los umbrales del SLA se cumplieron." -ForegroundColor Green
} else {
    Write-Host "  Prueba RECHAZADA: k6 salio con codigo $resultado." -ForegroundColor Red
    Write-Host "  En el pipeline, esto marca el build en rojo." -ForegroundColor Yellow
}
Write-Host ""
exit $resultado
