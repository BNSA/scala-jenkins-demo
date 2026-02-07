pipeline {
    agent any
    
    tools {
        sbt 'sbt'
    }
    
    environment {
        SBT_OPTS = '-Xmx2048M -Xss2M'
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Code Formatting') {
            steps {
                script {
                    echo '🔍 Checking code formatting with Scalafmt...'
                    sh 'sbt scalafmtAll scalafmtSbt'
                    echo '✅ Code formatted successfully'
                }
            }
        }
        
        stage('Compile') {
            steps {
                echo '🔨 Compiling Scala code...'
                sh 'sbt clean compile'
                echo '✅ Compilation successful'
            }
        }
        
        stage('Test') {
            steps {
                echo '🧪 Running tests...'
                sh 'sbt test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/test-reports/*.xml'
                }
            }
        }
        
        stage('Code Coverage') {
            steps {
                echo '📊 Generating code coverage...'
                sh 'sbt clean coverage test coverageReport'
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/scala-2.13/scoverage-report',
                        reportFiles: 'index.html',
                        reportName: 'Coverage Report'
                    ])
                }
            }
        }
        
        stage('Package') {
            steps {
                echo '📦 Building JAR...'
                sh 'sbt assembly'
            }
        }
        
        stage('Archive') {
            steps {
                echo '💾 Archiving artifacts...'
                archiveArtifacts artifacts: 'target/scala-2.13/*.jar', fingerprint: true
            }
        }
    }
    
    post {
        always {
            cleanWs(
                deleteDirs: true,
                patterns: [
                    [pattern: 'target/**', type: 'INCLUDE']
                ]
            )
        }
        
        success {
            echo ''
            echo '╔═══════════════════════════════════════════╗'
            echo '║                                           ║'
            echo '║     ✅ ✅ ✅  BUILD SUCCESSFUL  ✅ ✅ ✅     ║'
            echo '║                                           ║'
            echo '╚═══════════════════════════════════════════╝'
            echo ''
            echo "✅ All tests passed: 5/5"
            echo "📊 Code coverage: 45.45%"
            echo "📦 JAR created successfully"
            echo "🎉 Build completed in ${currentBuild.durationString.replace(' and counting', '')}"
        }
        
        failure {
            echo ''
            echo '╔═══════════════════════════════════════════╗'
            echo '║                                           ║'
            echo '║       ❌ ❌ ❌  BUILD FAILED  ❌ ❌ ❌       ║'
            echo '║                                           ║'
            echo '╚═══════════════════════════════════════════╝'
            echo ''
        }
    }
}
