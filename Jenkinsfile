pipeline {
    agent any
    
    options {
        timeout(time: 20, unit: 'MINUTES')
    }
    
    stages {
        stage('Build and Test') {
            steps {
                sh '''
                    export SBT_OPTS="-Xmx2048M -Xss2M"
                    sbt clean compile test
                '''
            }
        }
    }
    
    post {
        success {
            echo '✅ Build successful!'
        }
    }
}
