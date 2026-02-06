pipeline {
    agent any
    
    stages {
        stage('Build and Test') {
            steps {
                sh 'sbt -Dsbt.log.noformat=true -batch clean compile test'
            }
        }
    }
    
    post {
        success {
            echo '✅ Build Successful - All tests passed!'
        }
    }
}
