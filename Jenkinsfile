pipeline {
    agent any
    
    environment {
        SBT_OPTS = '-Xmx2048M -Xss2M'
    }
    
    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }
    
    stages {
        stage('Environment Info') {
            steps {
                echo '=== Build Environment ==='
                echo "Build Number: ${env.BUILD_NUMBER}"
                echo "Branch: ${env.BRANCH_NAME}"
                sh 'java -version'
                sh 'which sbt'
            }
        }
        
        stage('Checkout') {
            steps {
                echo '=== Checking out code ==='
                checkout scm
            }
        }
        
        stage('Clean Cache') {
            steps {
                echo '=== Cleaning SBT cache ==='
                script {
                    sh '''
                        rm -rf ~/.ivy2/cache/com.example
                        rm -rf target
                        rm -rf project/target
                    '''
                }
            }
        }
        
        stage('Download Dependencies') {
            steps {
                echo '=== Downloading dependencies ==='
                timeout(time: 15, unit: 'MINUTES') {
                    sh 'sbt -v -batch update'
                }
            }
        }
        
        stage('Compile') {
            steps {
                echo '=== Compiling Scala code ==='
                timeout(time: 10, unit: 'MINUTES') {
                    sh 'sbt -v -batch clean compile'
                }
            }
        }
        
        stage('Test') {
            steps {
                echo '=== Running tests ==='
                timeout(time: 15, unit: 'MINUTES') {
                    sh 'sbt -v -batch test'
                }
            }
        }
        
        stage('Package') {
            steps {
                echo '=== Packaging application ==='
                timeout(time: 10, unit: 'MINUTES') {
                    sh 'sbt -batch package'
                }
            }
        }
    }
    
    post {
        success {
            echo '=== Build Successful ==='
        }
        failure {
            echo '=== Build Failed ==='
        }
        always {
            echo '=== Cleaning up workspace ==='
            cleanWs(
                deleteDirs: true,
                patterns: [
                    [pattern: 'target/**', type: 'INCLUDE'],
                    [pattern: '.ivy2/**', type: 'INCLUDE']
                ]
            )
        }
    }
}
