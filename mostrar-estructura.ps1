# ---------------------------------------------------------------------------
#  Muestra la estructura del proyecto: solo los archivos versionados.
#
#  Se usa 'git ls-files' en lugar de 'tree' porque asi quedan fuera, sin
#  necesidad de filtros, las carpetas que no forman parte del proyecto:
#  .git, target (artefactos del build), sitio y node_modules.
#
#  Uso:   .\mostrar-estructura.ps1
# ---------------------------------------------------------------------------

Set-Location $PSScriptRoot

# Carpetas cuyo contenido se resume en una linea, para que el arbol quepa
# en pantalla sin perder la vista general.
$colapsar = @('docs/evidencias')

$archivos = git ls-files | Where-Object { $_ }

# Se reemplazan los archivos de las carpetas colapsadas por una sola entrada.
$listado = New-Object System.Collections.Generic.List[string]
$resumidas = @{}
foreach ($f in $archivos) {
    $carpeta = $colapsar | Where-Object { $f.StartsWith("$_/") } | Select-Object -First 1
    if ($carpeta) {
        if (-not $resumidas.ContainsKey($carpeta)) { $resumidas[$carpeta] = 0 }
        $resumidas[$carpeta]++
    } else {
        $listado.Add($f)
    }
}
foreach ($c in $resumidas.Keys) { $listado.Add("$c/<$($resumidas[$c]) archivos de evidencia>") }

# Construccion del arbol
$arbol = @{}
foreach ($ruta in ($listado | Sort-Object)) {
    $partes = $ruta -split '/'
    $nodo = $arbol
    foreach ($p in $partes) {
        if (-not $nodo.ContainsKey($p)) { $nodo[$p] = @{} }
        $nodo = $nodo[$p]
    }
}

function Dibujar($nodo, $prefijo) {
    $claves = $nodo.Keys | Sort-Object { if ($nodo[$_].Count -gt 0) { "0$_" } else { "1$_" } }
    $total = $claves.Count
    $i = 0
    foreach ($k in $claves) {
        $i++
        $ultimo = ($i -eq $total)
        $rama = if ($ultimo) { '\-- ' } else { '+-- ' }
        $esCarpeta = $nodo[$k].Count -gt 0
        $color = if ($esCarpeta) { 'Cyan' } else { 'Gray' }
        $nombre = if ($esCarpeta) { "$k/" } else { $k }
        Write-Host "$prefijo$rama" -NoNewline
        Write-Host $nombre -ForegroundColor $color
        if ($esCarpeta) {
            $siguiente = if ($ultimo) { "$prefijo    " } else { "$prefijo|   " }
            Dibujar $nodo[$k] $siguiente
        }
    }
}

Write-Host ""
Write-Host "taller1-ci-bdd/" -ForegroundColor Yellow
Dibujar $arbol ""
Write-Host ""
Write-Host "  $($archivos.Count) archivos versionados. No se listan target/, sitio/ ni .git/," -ForegroundColor DarkGray
Write-Host "  porque son generados por el build y estan excluidos en .gitignore." -ForegroundColor DarkGray
Write-Host ""
