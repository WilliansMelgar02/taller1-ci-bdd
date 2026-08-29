# ---------------------------------------------------------------------------
#  Guarda la imagen del portapapeles como evidencia del taller.
#
#  Flujo:
#    1. Toma la captura con  Win + Shift + S  (queda en el portapapeles)
#    2. Ejecuta:  .\guardar-captura.ps1 01-mvn-test-local
#
#  La imagen se guarda en docs\evidencias\<nombre>.png con el nombre exacto
#  que espera el informe, reemplazando la versión anterior.
# ---------------------------------------------------------------------------
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Nombre
)

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$imagen = [System.Windows.Forms.Clipboard]::GetImage()

if ($null -eq $imagen) {
    Write-Host ""
    Write-Host "  No hay ninguna imagen en el portapapeles." -ForegroundColor Red
    Write-Host "  Toma la captura primero con  Win + Shift + S  y vuelve a ejecutar el comando."
    Write-Host ""
    exit 1
}

$carpeta = Join-Path $PSScriptRoot "docs\evidencias"
if (-not (Test-Path $carpeta)) { New-Item -ItemType Directory -Path $carpeta | Out-Null }

$destino = Join-Path $carpeta "$($Nombre -replace '\.png$', '').png"
$imagen.Save($destino, [System.Drawing.Imaging.ImageFormat]::Png)
$imagen.Dispose()

$info = Get-Item $destino
Write-Host ""
Write-Host "  Captura guardada" -ForegroundColor Green
Write-Host "    archivo : $destino"
Write-Host "    tamano  : $([math]::Round($info.Length / 1KB, 1)) KB"
Write-Host ""
