pipeline {
    agent any
    
    options {
        timeout(time: 20, unit: 'MINUTES')
    }
    
    stages {
        stage('Clean Workspace') {
            steps {
                sh '''
                    rm -rf target
                    rm -rf project/target
                    rm -rf project/project
                '''
            }
        }
        
        stage('Compile') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    sh '''
                        export SBT_OPTS="-Xmx2048M -Xss2M -Dsbt.log.noformat=true"
                        sbt -batch -no-colors ";clean;compile"
                    '''
                }
            }
        }
        
        stage('Test') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    sh '''
                        export SBT_OPTS="-Xmx2048M -Xss2M -Dsbt.log.noformat=true"
                        sbt -batch -no-colors test
                    '''
                }
            }
        }
    }
    
    post {
        success {
            echo 'Build completed successfully!'
        }
        failure {
            echo 'Build failed!'
        }
    }
}
