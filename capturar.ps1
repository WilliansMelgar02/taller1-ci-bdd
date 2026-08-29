# ---------------------------------------------------------------------------
#  Captura la ventana que esté al frente y la guarda como evidencia.
#
#  Uso:   .\capturar.ps1 01-mvn-test-local
#
#  Da 5 segundos para que pongas al frente la ventana que quieres fotografiar.
#  La imagen se guarda en docs\evidencias\<nombre>.png
# ---------------------------------------------------------------------------
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Nombre,

    [int]$Segundos = 5,

    [switch]$PantallaCompleta
)

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms

# API de Windows para conocer la ventana en primer plano y sus dimensiones.
if (-not ('Ventana' -as [type])) {
    Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Ventana {
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT { public int Left, Top, Right, Bottom; }
}
"@
}

# Se recuerda la posición del cursor para poder borrar despues los mensajes de
# este script: si quedaran escritos, apareceria en la evidencia el aviso de la
# cuenta regresiva en lugar de terminar limpio en el prompt.
$posicionInicial = $Host.UI.RawUI.CursorPosition

Write-Host ""
Write-Host "  Pon al frente la ventana que quieres capturar." -ForegroundColor Cyan
for ($i = $Segundos; $i -ge 1; $i--) {
    Write-Host "`r  Capturando en $i segundos... " -NoNewline -ForegroundColor Yellow
    Start-Sleep -Seconds 1
}

# Borrar los mensajes propios y devolver el cursor al prompt
try {
    $posicionFinal = $Host.UI.RawUI.CursorPosition
    $anchoConsola = [Console]::WindowWidth - 1
    for ($fila = $posicionInicial.Y; $fila -le $posicionFinal.Y; $fila++) {
        [Console]::SetCursorPosition(0, $fila)
        Write-Host (' ' * $anchoConsola) -NoNewline
    }
    [Console]::SetCursorPosition($posicionInicial.X, $posicionInicial.Y)
} catch { }

Start-Sleep -Milliseconds 250

if ($PantallaCompleta) {
    $area = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    $x = $area.X; $y = $area.Y; $ancho = $area.Width; $alto = $area.Height
} else {
    $hwnd = [Ventana]::GetForegroundWindow()
    $r = New-Object Ventana+RECT
    [void][Ventana]::GetWindowRect($hwnd, [ref]$r)
    $x = $r.Left; $y = $r.Top
    $ancho = $r.Right - $r.Left
    $alto  = $r.Bottom - $r.Top
    if ($ancho -le 0 -or $alto -le 0) {
        Write-Host "  No se pudo medir la ventana; se captura la pantalla completa." -ForegroundColor Yellow
        $area = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
        $x = $area.X; $y = $area.Y; $ancho = $area.Width; $alto = $area.Height
    }
}

$bmp = New-Object System.Drawing.Bitmap $ancho, $alto
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($x, $y, 0, 0, $bmp.Size)
$g.Dispose()

$carpeta = Join-Path $PSScriptRoot "docs\evidencias"
if (-not (Test-Path $carpeta)) { New-Item -ItemType Directory -Path $carpeta | Out-Null }
$destino = Join-Path $carpeta "$($Nombre -replace '\.png$','').png"

$bmp.Save($destino, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

$info = Get-Item $destino
Write-Host ""
Write-Host "  Captura guardada" -ForegroundColor Green
Write-Host "    archivo : $destino"
Write-Host "    tamano  : $ancho x $alto px  ($([math]::Round($info.Length/1KB,1)) KB)"
Write-Host ""
