pipeline {
    agent any
    
    environment {
        SBT_OPTS = '-Xmx1024M -Xss1M -XX:+UseG1GC'  // Reduced memory usage
        VERSION = "${BUILD_NUMBER}"
        SCANNER_HOME = tool 'SonarQube Scanner'
    }
    
    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 1: Checking out source code'
                echo '═══════════════════════════════════════════'
                checkout scm
                script {
                    try {
                        env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        env.GIT_AUTHOR = sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
                    } catch (Exception e) {
                        env.GIT_COMMIT_SHORT = 'unknown'
                        env.GIT_AUTHOR = 'unknown'
                    }
                }
                echo "✓ Commit: ${env.GIT_COMMIT_SHORT} by ${env.GIT_AUTHOR}"
            }
        }
        
        stage('Environment Info') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 2: Build Environment'
                echo '═══════════════════════════════════════════'
                sh '''
                    echo "Build Number: ${BUILD_NUMBER}"
                    echo "-------------------------------------------"
                    java -version
                    echo "-------------------------------------------"
                    free -h
                    echo "-------------------------------------------"
                '''
            }
        }
        
        stage('Code Formatting - Auto Fix') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 3: Auto-formatting with Scalafmt'
                echo '═══════════════════════════════════════════'
                sh 'sbt scalafmtAll scalafmtSbt'
                echo '✓ Code automatically formatted'
            }
        }
        
        stage('Compile') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 4: Compiling Scala Code'
                echo '═══════════════════════════════════════════'
                sh 'sbt compile Test/compile'
                echo '✓ Compilation successful'
            }
        }
        
        stage('Unit Tests') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 5: Running Unit Tests'
                echo '═══════════════════════════════════════════'
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
                echo '═══════════════════════════════════════════'
                echo '  Stage 6: Generating Code Coverage'
                echo '═══════════════════════════════════════════'
                sh 'sbt coverage test coverageReport'
            }
            post {
                always {
                    script {
                        try {
                            publishHTML([
                                allowMissing: false,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: 'target/scala-2.13/scoverage-report',
                                reportFiles: 'index.html',
                                reportName: 'Code Coverage Report'
                            ])
                            archiveArtifacts artifacts: 'target/scala-2.13/scoverage-report/**/*', allowEmptyArchive: true
                            echo '✓ Coverage report published'
                        } catch (Exception e) {
                            echo '⚠️ Coverage report not available'
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 7: SonarQube Code Analysis'
                echo '═══════════════════════════════════════════'
                script {
                    withSonarQubeEnv('SonarQube') {
                        sh '''
                            ${SCANNER_HOME}/bin/sonar-scanner \
                                -Dsonar.projectKey=scala-jenkins-demo \
                                -Dsonar.projectName="Scala Jenkins Demo" \
                                -Dsonar.projectVersion=${BUILD_NUMBER} \
                                -Dsonar.host.url=http://13.71.48.197:9000 \
                                -Dsonar.sources=src/main/scala \
                                -Dsonar.tests=src/test/scala \
                                -Dsonar.sourceEncoding=UTF-8 \
                                -Dsonar.scala.version=2.13 \
                                -Dsonar.scala.scoverage.reportPath=target/scala-2.13/scoverage-report/scoverage.xml
                        '''
                    }
                }
                echo '✓ SonarQube analysis completed'
            }
        }

        stage('Quality Gate') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 8: Checking Quality Gate'
                echo '═══════════════════════════════════════════'
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
                echo '✓ Quality Gate check completed'
            }
        }
        
        stage('Package JAR') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 9: Packaging Standard JAR'
                echo '═══════════════════════════════════════════'
                sh 'sbt package'
                echo '✓ JAR created'
            }
        }
        
        stage('Build Fat JAR') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 10: Building Fat JAR (Assembly)'
                echo '═══════════════════════════════════════════'
                sh 'sbt assembly'
                echo '✓ Fat JAR created'
            }
        }
        
        stage('Archive Artifacts') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 11: Archiving Build Artifacts'
                echo '═══════════════════════════════════════════'
                script {
                    archiveArtifacts artifacts: 'target/scala-2.13/*.jar', fingerprint: true, allowEmptyArchive: true
                    
                    sh """
cat > build-info.txt << 'EOF'
╔════════════════════════════════════════════════════════╗
║           BUILD INFORMATION                            ║
╠════════════════════════════════════════════════════════╣
║ Build Number    : ${BUILD_NUMBER}
║ Build Date      : \$(date '+%Y-%m-%d %H:%M:%S')
║ Git Commit      : ${env.GIT_COMMIT_SHORT}
║ Git Author      : ${env.GIT_AUTHOR}
║ Jenkins Job     : ${JOB_NAME}
╚════════════════════════════════════════════════════════╝

QUALITY METRICS:
─────────────────────────────────────────────────────────
✓ Code Formatting   : Auto-fixed with Scalafmt
✓ Compilation       : Successful
✓ Unit Tests        : All passed
✓ Code Coverage     : Generated
✓ SonarQube         : Analysis completed
✓ Quality Gate      : Checked
✓ JAR Packaging     : Completed

ARTIFACTS:
─────────────────────────────────────────────────────────
- Standard JAR: target/scala-2.13/scala-jenkins-demo_2.13-1.0.0.jar
- Fat JAR:      target/scala-2.13/scala-jenkins-demo-1.0.0-assembly.jar

BUILD SUCCESSFUL ✅
EOF
                    """
                    archiveArtifacts artifacts: 'build-info.txt', fingerprint: true
                    sh 'cat build-info.txt'
                }
            }
        }
    }
    
    post {
        always {
            echo ''
            echo '═══════════════════════════════════════════'
            echo '  PIPELINE EXECUTION COMPLETED'
            echo '═══════════════════════════════════════════'
            script {
                def duration = currentBuild.durationString.replace(' and counting', '')
                echo "Total Duration: ${duration}"
            }
        }
        
        success {
            echo ''
            echo '╔═══════════════════════════════════════════╗'
            echo '║                                           ║'
            echo '║     ✅ ✅ ✅  BUILD SUCCESSFUL  ✅ ✅ ✅     ║'
            echo '║                                           ║'
            echo '╚═══════════════════════════════════════════╝'
            echo ''
            echo '📊 Build Summary:'
            echo '  • All 11 stages completed successfully'
            echo '  • Code auto-formatted with Scalafmt'
            echo '  • All unit tests passed'
            echo '  • Code coverage report generated'
            echo '  • SonarQube analysis completed'
            echo '  • Quality Gate checked'
            echo '  • JAR artifacts created and archived'
            echo ''
            echo "🎉 Ready for demo!"
        }
        
        failure {
            echo ''
            echo '╔═══════════════════════════════════════════╗'
            echo '║                                           ║'
            echo '║       ❌ ❌ ❌  BUILD FAILED  ❌ ❌ ❌       ║'
            echo '║                                           ║'
            echo '╚═══════════════════════════════════════════╝'
            echo ''
            echo "Failed at stage: ${env.STAGE_NAME}"
        }
    }
}

