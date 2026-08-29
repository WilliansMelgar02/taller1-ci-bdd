// =============================================================================
//  Jenkinsfile — alternativa on-premise al pipeline de GitHub Actions.
//
//  Se incluye para demostrar que la estrategia de CI es independiente de la
//  herramienta: las mismas etapas (compilar → unitarias → BDD → performance →
//  reportes → alertas) se expresan en Jenkins con Declarative Pipeline.
//
//  Plugins requeridos: Pipeline, JUnit, HTML Publisher, Performance,
//  Warnings NG (opcional) y Mailer / Slack Notification.
// =============================================================================
pipeline {

    agent any

    tools {
        jdk   'jdk-17'          // configurados en "Manage Jenkins → Tools"
        maven 'maven-3.9'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30'))
        timestamps()
        disableConcurrentBuilds()
    }

    // El pipeline se dispara con cada push (webhook) o, como respaldo,
    // consultando el SCM cada 5 minutos.
    triggers {
        pollSCM('H/5 * * * *')
    }

    environment {
        UMBRAL_P95_MS      = '800'    // latencia p95 máxima aceptada
        UMBRAL_ERRORES_PCT = '1'      // tasa de error máxima aceptada
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                echo "Construyendo la rama ${env.BRANCH_NAME} — commit ${env.GIT_COMMIT}"
            }
        }

        stage('Compilar') {
            steps {
                sh 'mvn -B -ntp clean compile'
            }
        }

        stage('Pruebas unitarias') {
            steps {
                sh 'mvn -B -ntp test'
            }
            post {
                always {
                    // Publica los resultados y marca el build UNSTABLE si hay fallos.
                    junit testResults: 'target/surefire-reports/TEST-*.xml',
                          allowEmptyResults: false
                }
            }
        }

        stage('Escenarios BDD') {
            steps {
                sh 'mvn -B -ntp verify -DskipUnitTests=true'
            }
            post {
                always {
                    junit testResults: 'target/cucumber-reports/cucumber-junit.xml',
                          allowEmptyResults: false
                }
            }
        }

        stage('Prueba de performance') {
            steps {
                sh 'node performance/servidor-mock.js & sleep 3'
                sh 'k6 run --summary-export=performance/resultados/resumen-k6.json performance/login-carga.js'
            }
        }

        stage('Publicar reportes navegables') {
            steps {
                sh 'mvn -B -ntp site -DskipTests'
                publishHTML(target: [
                    reportDir            : 'target/cucumber-reports',
                    reportFiles          : 'reporte-bdd.html',
                    reportName           : 'Reporte BDD (Cucumber)',
                    keepAll              : true,
                    alwaysLinkToLastBuild: true,
                    allowMissing         : false
                ])
                publishHTML(target: [
                    reportDir            : 'target/site',
                    reportFiles          : 'surefire.html,failsafe.html',
                    reportName           : 'Reporte de pruebas (Maven Site)',
                    keepAll              : true,
                    alwaysLinkToLastBuild: true,
                    allowMissing         : true
                ])
                archiveArtifacts artifacts: 'target/cucumber-reports/**, performance/resultados/**',
                                 fingerprint: true
            }
        }
    }

    // =========================================================================
    //  Alertas automáticas: el pipeline avisa, nadie tiene que ir a mirarlo.
    // =========================================================================
    post {
        failure {
            mail to: 'equipo-qa@empresa.cl',
                 subject: "[CI] FALLO en ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: """El pipeline falló en la rama ${env.BRANCH_NAME}.
                          Revisar: ${env.BUILD_URL}console"""
            slackSend channel: '#alertas-qa',
                      color: 'danger',
                      message: ":rotating_light: Build #${env.BUILD_NUMBER} FALLÓ en ${env.BRANCH_NAME} — ${env.BUILD_URL}"
        }
        unstable {
            slackSend channel: '#alertas-qa',
                      color: 'warning',
                      message: ":warning: Build #${env.BUILD_NUMBER} INESTABLE (pruebas fallidas) — ${env.BUILD_URL}testReport"
        }
        fixed {
            slackSend channel: '#alertas-qa',
                      color: 'good',
                      message: ":white_check_mark: Build #${env.BUILD_NUMBER} recuperado en ${env.BRANCH_NAME}"
        }
        always {
            cleanWs()
        }
    }
}
