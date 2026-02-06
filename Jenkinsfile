pipeline {
    agent any
    
    stages {
        stage('Environment Info') {
            steps {
                echo '=== Build Environment ==='
                echo "Build Number: ${env.BUILD_NUMBER}"
                echo "Branch: ${env.BRANCH_NAME}"
                sh 'java -version'
                sh 'which sbt || echo "SBT not found in PATH"'
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
                    junit allowEmptyResults: true, testResults: '**/target/test-reports/*.xml'
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
            echo "=== Pipeline completed ==="
        }
        success {
            echo '✅ BUILD SUCCESSFUL!'
        }
        failure {
            echo '❌ BUILD FAILED!'
        }
    }
}
