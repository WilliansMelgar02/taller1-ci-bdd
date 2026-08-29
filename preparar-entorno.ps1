# ---------------------------------------------------------------------------
#  Prepara la sesión de PowerShell para trabajar con el proyecto.
#
#  Maven y k6 no están instalados en el sistema: viven en .local-tools, así que
#  este script los agrega al PATH solo para esta ventana de terminal (no toca
#  la configuración global de Windows).
#
#  Uso:   . .\preparar-entorno.ps1      <- ojo con el punto y el espacio inicial
# ---------------------------------------------------------------------------

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;" +
            "C:\Users\Lancenterstore\.local-tools\apache-maven-3.9.9\bin;" +
            "C:\Users\Lancenterstore\.local-tools\k6-v0.55.0-windows-amd64;" +
            $env:PATH

Write-Host ""
Write-Host "  Entorno preparado" -ForegroundColor Green
Write-Host "  ------------------------------------------------"
Write-Host "  java  : $(@(java -version 2>&1)[0])"
Write-Host "  maven : $(@(mvn -v 2>&1)[0])"
Write-Host "  k6    : $(@(k6 version 2>&1)[0])"
Write-Host "  node  : $(node --version)"
Write-Host ""
Write-Host "  Comandos disponibles:" -ForegroundColor Cyan
Write-Host "    mvn clean test                   Pruebas unitarias"
Write-Host "    mvn verify -DskipUnitTests=true  Escenarios BDD"
Write-Host "    k6 run performance\login-carga.js Prueba de carga"
Write-Host "    git log --graph --oneline --all  Arbol de ramas"
Write-Host ""
