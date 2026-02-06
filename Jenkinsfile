pipeline {
    agent any

    options {
        disableConcurrentBuilds()           // Prevents overlapping builds on the same job
        disableResume()                     // ← Critical: stops "resuming after restart" hangs
        timeout(time: 20, unit: 'MINUTES')  // Auto-abort if stuck too long (adjust as needed)
    }

    stages {
        stage('Environment Info') {
            steps {
                echo '=== Build Environment ==='
                echo "Build Number: ${env.BUILD_NUMBER}"
                echo "Branch: ${env.BRANCH_NAME ?: 'main'}"
                sh 'java -version'
                sh 'which sbt || echo "SBT not found in PATH"'
                sh 'sbt --version || echo "sbt --version failed"'
            }
        }

        stage('Checkout') {
            steps {
                echo '=== Checking out code ==='
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                echo '=== Compiling Scala code ==='
                sh 'sbt clean compile'
            }
        }

        stage('Test') {
            steps {
                echo '=== Running tests ==='
                sh 'sbt test'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/test-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo '=== Packaging application ==='
                sh 'sbt package'
            }
        }
    }

    post {
        always {
            cleanWs()  // Cleans workspace after every build (good for sbt)
            echo "=== Pipeline completed ==="
        }
        success {
            echo '✅ BUILD SUCCESSFUL!'
        }
        failure {
            echo '❌ BUILD FAILED!'
        }
        aborted {
            echo '⚠️ BUILD ABORTED!'
        }
    }
}
