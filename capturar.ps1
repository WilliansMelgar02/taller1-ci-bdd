# ---------------------------------------------------------------------------
#  Captura una ventana y la guarda como evidencia del taller.
#
#  Tres formas de usarlo:
#
#    .\capturar.ps1 01-mvn-test-local
#        Captura esta misma ventana de PowerShell (lo habitual).
#
#    .\capturar.ps1 12-pipeline -Ventana "GitHub"
#        Busca la ventana cuyo titulo contenga "GitHub", la trae al frente y
#        la fotografia. No hay que cambiar de ventana a mano.
#
#    .\capturar.ps1 15-todo -PantallaCompleta
#        Captura la pantalla completa.
#
#  La imagen se guarda en docs\evidencias\<nombre>.png
# ---------------------------------------------------------------------------
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Nombre,

    # Texto contenido en el titulo de la ventana a capturar.
    [string]$Ventana,

    [switch]$PantallaCompleta,

    # Solo se usa cuando no se indica -Ventana: da tiempo a cambiar de ventana.
    [int]$Segundos = 0
)

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms

# API de Windows: enumerar ventanas, traerlas al frente y medirlas.
if (-not ('Win32' -as [type])) {
    Add-Type @"
using System;
using System.Text;
using System.Collections.Generic;
using System.Runtime.InteropServices;

public class Win32 {
    public delegate bool EnumProc(IntPtr hWnd, IntPtr lParam);

    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, IntPtr lParam);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern int  GetWindowTextLength(IntPtr hWnd);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern int GetWindowText(IntPtr hWnd, StringBuilder text, int count);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT r);

    [StructLayout(LayoutKind.Sequential)]
    public struct RECT { public int Left, Top, Right, Bottom; }

    public static List<KeyValuePair<IntPtr,string>> Listar() {
        var lista = new List<KeyValuePair<IntPtr,string>>();
        EnumWindows(delegate(IntPtr h, IntPtr p) {
            if (!IsWindowVisible(h)) return true;
            int largo = GetWindowTextLength(h);
            if (largo == 0) return true;
            var sb = new StringBuilder(largo + 1);
            GetWindowText(h, sb, sb.Capacity);
            lista.Add(new KeyValuePair<IntPtr,string>(h, sb.ToString()));
            return true;
        }, IntPtr.Zero);
        return lista;
    }
}
"@
}

$destinoHwnd = [IntPtr]::Zero

if ($Ventana) {
    # Buscar la ventana por una parte de su titulo.
    $candidatas = [Win32]::Listar() | Where-Object { $_.Value -like "*$Ventana*" }

    if (-not $candidatas) {
        Write-Host ""
        Write-Host "  No se encontro ninguna ventana cuyo titulo contenga '$Ventana'." -ForegroundColor Red
        Write-Host "  Ventanas abiertas en este momento:" -ForegroundColor Yellow
        [Win32]::Listar() | ForEach-Object { Write-Host "    - $($_.Value)" }
        Write-Host ""
        exit 1
    }

    $elegida = @($candidatas)[0]
    $destinoHwnd = $elegida.Key
    Write-Host ""
    Write-Host "  Capturando la ventana: $($elegida.Value)" -ForegroundColor Cyan

    [void][Win32]::ShowWindow($destinoHwnd, 9)          # 9 = restaurar si esta minimizada
    [void][Win32]::SetForegroundWindow($destinoHwnd)
    Start-Sleep -Milliseconds 700                        # dejar que se dibuje
}
else {
    # Sin -Ventana: se captura la ventana en primer plano (esta misma consola).
    $posicionInicial = $Host.UI.RawUI.CursorPosition

    if ($Segundos -gt 0) {
        Write-Host ""
        Write-Host "  Pon al frente la ventana que quieres capturar." -ForegroundColor Cyan
        for ($i = $Segundos; $i -ge 1; $i--) {
            Write-Host "`r  Capturando en $i segundos... " -NoNewline -ForegroundColor Yellow
            Start-Sleep -Seconds 1
        }
        # Borrar los mensajes propios para que no salgan en la evidencia.
        try {
            $posicionFinal = $Host.UI.RawUI.CursorPosition
            $ancho = [Console]::WindowWidth - 1
            for ($fila = $posicionInicial.Y; $fila -le $posicionFinal.Y; $fila++) {
                [Console]::SetCursorPosition(0, $fila)
                Write-Host (' ' * $ancho) -NoNewline
            }
            [Console]::SetCursorPosition($posicionInicial.X, $posicionInicial.Y)
        } catch { }
        Start-Sleep -Milliseconds 250
    }

    $destinoHwnd = [Win32]::GetForegroundWindow()
}

# --- Determinar el area a capturar ------------------------------------------
if ($PantallaCompleta) {
    $area = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    $x = $area.X; $y = $area.Y; $ancho = $area.Width; $alto = $area.Height
} else {
    $r = New-Object Win32+RECT
    [void][Win32]::GetWindowRect($destinoHwnd, [ref]$r)
    $x = $r.Left; $y = $r.Top
    $ancho = $r.Right - $r.Left
    $alto  = $r.Bottom - $r.Top
    if ($ancho -le 0 -or $alto -le 0) {
        $area = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
        $x = $area.X; $y = $area.Y; $ancho = $area.Width; $alto = $area.Height
    }
}

# --- Capturar y guardar -------------------------------------------------------
$bmp = New-Object System.Drawing.Bitmap $ancho, $alto
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($x, $y, 0, 0, $bmp.Size)
$g.Dispose()

$carpeta = Join-Path $PSScriptRoot "docs\evidencias"
if (-not (Test-Path $carpeta)) { New-Item -ItemType Directory -Path $carpeta | Out-Null }
$destino = Join-Path $carpeta "$($Nombre -replace '\.png$','').png"

$bmp.Save($destino, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

# Volver el foco a la consola para poder seguir escribiendo.
if ($Ventana) { Start-Sleep -Milliseconds 200 }

$info = Get-Item $destino
Write-Host ""
Write-Host "  Captura guardada" -ForegroundColor Green
Write-Host "    archivo : $destino"
Write-Host "    tamano  : $ancho x $alto px  ($([math]::Round($info.Length/1KB,1)) KB)"
Write-Host ""
