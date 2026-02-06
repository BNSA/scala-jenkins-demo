pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                sh '''
                    export SBT_OPTS="-Xmx2048M -Xss2M"
                    cd $WORKSPACE
                    /usr/bin/sbt clean compile test
                '''
            }
        }
    }
}
