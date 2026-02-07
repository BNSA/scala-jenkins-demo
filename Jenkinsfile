pipeline {
    agent any
    
    tools {
        jdk 'JDK17'
    }
    
    environment {
        SBT_OPTS = '-Xmx2G -Xss2M'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Code Formatting Check') {
            steps {
                script {
                    echo "Checking code formatting with Scalafmt..."
                    def formatCheckResult = sh(
                        script: 'sbt scalafmtCheckAll scalafmtSbtCheck',
                        returnStatus: true
                    )
                    
                    if (formatCheckResult != 0) {
                        echo "⚠️  Code formatting issues found!"
                        echo "Run 'sbt scalafmtAll scalafmtSbt' to fix formatting"
                        unstable(message: "Code formatting check failed")
                    } else {
                        echo "✅ Code formatting check passed"
                    }
                }
            }
        }
        
        stage('Compile') {
            steps {
                sh 'sbt clean compile'
            }
        }
        
        stage('Test') {
            steps {
                sh 'sbt test'
            }
            post {
                always {
                    junit '**/target/test-reports/*.xml'
                }
            }
        }
        
        stage('Code Coverage') {
            steps {
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
                sh 'sbt assembly'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/scala-2.13/*.jar', fingerprint: true
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
        unstable {
            echo '⚠️  Pipeline completed with warnings (check formatting issues)'
        }
    }
}
