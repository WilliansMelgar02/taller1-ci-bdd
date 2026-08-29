# ---------------------------------------------------------------------------
#  Prepara la sesión de PowerShell para trabajar con el proyecto.
#
#  Maven y k6 no vienen instalados en Windows: viven en C:\Herramientas.
#  Este script los agrega al PATH solo para esta ventana de terminal, sin
#  modificar la configuracion global del sistema.
#
#  Uso:   . .\preparar-entorno.ps1      <- ojo con el punto y el espacio inicial
# ---------------------------------------------------------------------------

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;" +
            "C:\Herramientas\apache-maven-3.9.9\bin;" +
            "C:\Herramientas\k6-v0.55.0-windows-amd64;" +
            $env:PATH


# La consola de Windows usa por defecto una pagina de codigos antigua (850/1252),
# mientras que Cucumber y Maven escriben en UTF-8. Sin esto, los acentos y la enie
# de los escenarios en espanol salen como "contraseÃ±a" en lugar de "contrasena".
chcp 65001 > $null
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$env:MAVEN_OPTS = "-Dfile.encoding=UTF-8"
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
