// =============================================================================
//  Jenkinsfile — Integración continua + Deployment pipeline Blue-Green
//
//  Equivalente on-premise de .github/workflows/ci.yml y despliegue.yml. Las
//  acciones de despliegue viven en infra/*.sh, de modo que Jenkins y GitHub
//  Actions ejecutan exactamente los mismos pasos: la estrategia no depende de
//  la herramienta de CI.
//
//    Commit stage ........ Checkout → Compilar → Análisis estático → Unitarias
//                          → Integración + BDD → Performance
//    Deployment pipeline . Empaquetar imagen → Deploy to Staging (Blue-Green)
//                          → Acceptance Gate → Cambiar tráfico → Promover
//    post { failure } .... infra/rollback.sh
//
//  Flujo de ramas Trunk-Based: el commit stage corre en toda rama y Pull
//  Request; el despliegue en staging, en main y en los PR hacia main; la
//  promoción a estable, solo en main.
//
//  Agente (label 'docker'): JDK 17, Maven 3.9, Docker, Google Chrome, Node.js y k6.
//  Plugins: Pipeline, Git, JUnit, HTML Publisher, Credentials Binding, Workspace
//  Cleanup y Slack Notification.
//  Credencial 'ghcr' (usuario + token con permiso write:packages).
// =============================================================================
pipeline {

    // Agente con Docker: los ambientes de prueba se crean y destruyen en cada ejecución.
    agent { label 'docker' }

    tools {
        jdk   'jdk-17'          // configurados en "Manage Jenkins → Tools"
        maven 'maven-3.9'
    }

    options {
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30'))
        timestamps()
        // Un solo despliegue a la vez: dos ejecuciones compartirían los puertos de staging.
        disableConcurrentBuilds()
    }

    // El pipeline se dispara con cada push (webhook) o, como respaldo,
    // consultando el SCM cada 5 minutos.
    triggers {
        pollSCM('H/5 * * * *')
    }

    environment {
        REGISTRO           = 'ghcr.io'
        REPOSITORIO_IMAGEN = 'ghcr.io/williansmelgar02/portal-clientes'
        DIRECTORIO_ESTADO  = "${WORKSPACE}/.staging"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    def commitCorto = sh(returnStdout: true, script: 'git rev-parse --short=7 HEAD').trim()
                    env.VERSION_BASE = sh(returnStdout: true,
                            script: 'mvn -B -ntp -q help:evaluate -Dexpression=project.version -DforceStdout').trim()
                    env.VERSION = "${env.VERSION_BASE}-${commitCorto}"
                    env.IMAGEN = "${env.REPOSITORIO_IMAGEN}:${env.VERSION}"
                }
                echo "Rama ${env.BRANCH_NAME} — versión ${env.VERSION}"
            }
        }

        // =====================================================================
        //  COMMIT STAGE: feedback rápido, de la etapa más barata a la más cara.
        // =====================================================================
        stage('Compilar') {
            steps {
                sh 'mvn -B -ntp clean compile'
            }
        }

        // SAST después del build y antes de las pruebas (ME_6): falla rápido y barato.
        stage('Análisis estático') {
            steps {
                sh 'mvn -B -ntp spotbugs:check'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/reports/spotbugs.html, target/spotbugsXml.xml', allowEmptyArchive: true
                }
            }
        }

        stage('Pruebas unitarias') {
            steps {
                sh 'mvn -B -ntp test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/TEST-*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Integración y escenarios BDD') {
            steps {
                // Vuelve a pasar por las unitarias para que JaCoCo mida la cobertura combinada.
                sh 'mvn -B -ntp verify'
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/TEST-*.xml', allowEmptyResults: false
                    publishHTML(target: [
                        reportDir            : 'target/cucumber-reports',
                        reportFiles          : 'reporte-bdd.html',
                        reportName           : 'Escenarios BDD (Cucumber)',
                        keepAll              : true,
                        alwaysLinkToLastBuild: true,
                        allowMissing         : true
                    ])
                    publishHTML(target: [
                        reportDir            : 'target/site/jacoco',
                        reportFiles          : 'index.html',
                        reportName           : 'Cobertura (JaCoCo)',
                        keepAll              : true,
                        alwaysLinkToLastBuild: true,
                        allowMissing         : true
                    ])
                }
            }
        }

        stage('Prueba de performance') {
            steps {
                // Se mide el jar real. Portal y prueba en el MISMO 'sh': Jenkins
                // termina los procesos en segundo plano al finalizar cada paso.
                sh '''
                    mvn -B -ntp package -DskipTests
                    mkdir -p performance/resultados
                    java -jar target/portal-clientes.jar > performance/resultados/portal.log 2>&1 &
                    PORTAL=$!
                    trap 'kill $PORTAL' EXIT
                    for i in $(seq 1 30); do
                      curl -sf http://localhost:8080/health > /dev/null && break
                      sleep 1
                    done
                    k6 run performance/login-carga.js
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'performance/resultados/**', allowEmptyArchive: true
                }
            }
        }

        // =====================================================================
        //  DEPLOYMENT PIPELINE
        // =====================================================================
        stage('Empaquetar imagen') {
            when {
                anyOf { branch 'main'; changeRequest target: 'main' }
            }
            steps {
                // Las pruebas ya corrieron en el commit stage de esta misma ejecución.
                sh 'mvn -B -ntp package -DskipTests'
                withCredentials([usernamePassword(credentialsId: 'ghcr',
                        usernameVariable: 'USUARIO_REGISTRO', passwordVariable: 'TOKEN_REGISTRO')]) {
                    sh 'echo "$TOKEN_REGISTRO" | docker login "$REGISTRO" -u "$USUARIO_REGISTRO" --password-stdin'
                }
                sh '''
                    docker build \
                      --build-arg VERSION="$VERSION" \
                      --build-arg COMMIT_SHA="$(git rev-parse HEAD)" \
                      --build-arg FECHA_BUILD="$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
                      --tag "$IMAGEN" .
                    ./infra/publicar-imagen.sh "$IMAGEN"
                '''
            }
        }

        stage('Deploy to Staging (Blue-Green)') {
            when {
                anyOf { branch 'main'; changeRequest target: 'main' }
            }
            steps {
                sh './infra/preparar-ambiente.sh'
                sh '''
                    if docker pull --quiet "$REPOSITORIO_IMAGEN:estable" > /dev/null 2>&1; then
                      ./infra/desplegar-estable.sh "$REPOSITORIO_IMAGEN:estable"
                    else
                      echo "Primera entrega: aún no existe una versión estable"
                    fi
                '''
                // Se marca ANTES de desplegar: si el arranque de GREEN falla, también hay que revertir.
                script { env.CANDIDATA_DESPLEGADA = 'true' }
                sh './infra/desplegar-candidata.sh "$IMAGEN"'
            }
        }

        stage('Acceptance Gate') {
            when {
                anyOf { branch 'main'; changeRequest target: 'main' }
            }
            steps {
                sh 'mvn -B -ntp verify -Paceptacion -Durl.base=http://localhost:8082 -Dversion.esperada="$VERSION"'
            }
            post {
                always {
                    junit testResults: 'target/aceptacion-reports/aceptacion-junit.xml', allowEmptyResults: true
                    publishHTML(target: [
                        reportDir            : 'target/aceptacion-reports',
                        reportFiles          : 'reporte-aceptacion.html',
                        reportName           : 'Acceptance Gate (Selenium + API)',
                        keepAll              : true,
                        alwaysLinkToLastBuild: true,
                        allowMissing         : true
                    ])
                }
            }
        }

        stage('Cambiar tráfico a GREEN') {
            when {
                anyOf { branch 'main'; changeRequest target: 'main' }
            }
            steps {
                sh './infra/cambiar-trafico.sh green'
                sh './infra/verificar-salud.sh http://localhost:8080 "$VERSION"'
            }
        }

        stage('Promover versión estable') {
            when { branch 'main' }
            steps {
                sh '''
                    for etiqueta in estable "$VERSION_BASE"; do
                      docker tag "$IMAGEN" "$REPOSITORIO_IMAGEN:$etiqueta"
                      ./infra/publicar-imagen.sh "$REPOSITORIO_IMAGEN:$etiqueta"
                    done
                '''
            }
        }
    }

    // =========================================================================
    //  Jenkins evalúa las condiciones de 'post' en orden fijo:
    //  always → fixed → failure → unstable → cleanup. Por eso el ambiente se
    //  destruye en 'cleanup' y no en 'always': el rollback de 'failure' lo necesita.
    // =========================================================================
    post {
        failure {
            script {
                if (env.CANDIDATA_DESPLEGADA == 'true') {
                    echo 'Despliegue fallido: iniciando rollback automático...'
                    sh './infra/rollback.sh'
                }
            }
            slackSend channel: '#alertas-qa',
                      color: 'danger',
                      message: "[FALLO] Build #${env.BUILD_NUMBER} FALLÓ en ${env.BRANCH_NAME} — ${env.BUILD_URL}"
        }
        unstable {
            slackSend channel: '#alertas-qa',
                      color: 'warning',
                      message: "[INESTABLE] Build #${env.BUILD_NUMBER} INESTABLE (pruebas fallidas) — ${env.BUILD_URL}testReport"
        }
        fixed {
            slackSend channel: '#alertas-qa',
                      color: 'good',
                      message: "[RECUPERADO] Build #${env.BUILD_NUMBER} recuperado en ${env.BRANCH_NAME}"
        }
        cleanup {
            script {
                if (fileExists('.staging')) {
                    sh './infra/estado-ambiente.sh || true'
                    archiveArtifacts artifacts: '.staging/**, target/aceptacion-reports/**', allowEmptyArchive: true
                    sh './infra/destruir-ambiente.sh'
                }
            }
            cleanWs()
        }
    }
}
