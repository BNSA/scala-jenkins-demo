pipeline {
    agent any
    
    tools {
        jdk 'JDK11'
    }
    
    environment {
        SBT_OPTS = '-Xmx2G -XX:+CMSClassUnloadingEnabled'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
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
    }
}
