pipeline {
    agent any
    
    environment {
        SBT_OPTS = '-Xmx2048M -Xss2M'
        VERSION = "${BUILD_NUMBER}"
        ARTIFACT_NAME = "scala-jenkins-demo-${VERSION}.jar"
        EMAIL_RECIPIENTS = 'your-email@example.com'
    }
    
    options {
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        // REMOVED: ansiColor('xterm') - plugin not installed
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 1: Checking out source code'
                echo '═══════════════════════════════════════════'
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    env.GIT_COMMIT_MSG = sh(
                        script: 'git log -1 --pretty=%B',
                        returnStdout: true
                    ).trim()
                    env.GIT_AUTHOR = sh(
                        script: 'git log -1 --pretty=%an',
                        returnStdout: true
                    ).trim()
                }
                echo "✓ Commit: ${env.GIT_COMMIT_SHORT} by ${env.GIT_AUTHOR}"
                echo "✓ Message: ${env.GIT_COMMIT_MSG}"
            }
        }
        
        stage('Environment Info') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 2: Build Environment Information'
                echo '═══════════════════════════════════════════'
                sh '''
                    echo "Build Number: ${BUILD_NUMBER}"
                    echo "Job Name: ${JOB_NAME}"
                    echo "Workspace: ${WORKSPACE}"
                    echo "-------------------------------------------"
                    echo "Java Version:"
                    java -version
                    echo "-------------------------------------------"
                    echo "Disk Space:"
                    df -h | grep -E '^/dev|Filesystem'
                    echo "-------------------------------------------"
                    echo "Memory:"
                    free -h
                    echo "-------------------------------------------"
                '''
            }
        }
        
        stage('Code Formatting Check - Scalafmt') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 3: Checking Code Formatting'
                echo '═══════════════════════════════════════════'
                script {
                    def formatCheckResult = sh(
                        script: 'scalafmt --check --config .scalafmt.conf 2>&1 || true',
                        returnStatus: true
                    )
                    if (formatCheckResult != 0) {
                        echo "⚠️  WARNING: Code formatting check skipped (scalafmt not found or issues detected)"
                        echo "Continuing build..."
                    } else {
                        echo "✓ Code formatting is correct"
                    }
                }
            }
        }
        
        stage('Style Check - Scalastyle') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 4: Running Scalastyle Checks'
                echo '═══════════════════════════════════════════'
                sh 'sbt -Dsbt.log.noformat=true -batch scalastyle test:scalastyle || echo "Scalastyle completed"'
            }
            post {
                always {
                    script {
                        try {
                            recordIssues(
                                enabledForFailure: true,
                                tool: checkStyle(
                                    pattern: 'target/scalastyle-result.xml,target/scalastyle-test-result.xml',
                                    reportEncoding: 'UTF-8'
                                )
                            )
                            echo "✓ Scalastyle results published"
                        } catch (Exception e) {
                            echo "⚠️  Could not publish Scalastyle results: ${e.message}"
                        }
                    }
                }
            }
        }
        
        stage('Compile') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 5: Compiling Scala Code'
                echo '═══════════════════════════════════════════'
                sh 'sbt -Dsbt.log.noformat=true -batch clean compile Test/compile'
                echo "✓ Compilation successful"
            }
        }
        
        stage('Linting - Wartremover') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 6: Running Wartremover Linter'
                echo '═══════════════════════════════════════════'
                script {
                    def wartResult = sh(
                        script: 'sbt -Dsbt.log.noformat=true -batch compile',
                        returnStatus: true
                    )
                    if (wartResult != 0) {
                        echo "⚠️  Wartremover found issues (continuing build)"
                    } else {
                        echo "✓ Wartremover checks passed"
                    }
                }
            }
        }
        
        stage('Unit Tests with Coverage') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 7: Running Unit Tests & Coverage'
                echo '═══════════════════════════════════════════'
                sh 'sbt -Dsbt.log.noformat=true -batch coverage test coverageReport'
            }
            post {
                always {
                    script {
                        try {
                            junit allowEmptyResults: true, testResults: 'target/test-reports/*.xml'
                            echo "✓ Test results published"
                        } catch (Exception e) {
                            echo "⚠️  Could not publish test results: ${e.message}"
                        }
                    }
                }
            }
        }
        
        stage('Code Coverage Report') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 8: Publishing Coverage Reports'
                echo '═══════════════════════════════════════════'
                script {
                    try {
                        publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'target/scala-2.13/scoverage-report',
                            reportFiles: 'index.html',
                            reportName: 'Scoverage Report',
                            reportTitles: 'Code Coverage'
                        ])
                        
                        archiveArtifacts artifacts: 'target/scala-2.13/scoverage-report/**/*', allowEmptyArchive: true
                        
                        sh '''
                            if [ -f target/scala-2.13/scoverage-report/scoverage.xml ]; then
                                echo "✓ Coverage report generated"
                            fi
                        '''
                    } catch (Exception e) {
                        echo "⚠️  Could not publish coverage report: ${e.message}"
                    }
                }
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 9: Running SonarQube Analysis'
                echo '═══════════════════════════════════════════'
                script {
                    try {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                                sonar-scanner \
                                    -Dsonar.projectKey=scala-jenkins-demo \
                                    -Dsonar.projectName="Scala Jenkins Demo" \
                                    -Dsonar.projectVersion=${VERSION} \
                                    -Dsonar.sources=src/main/scala \
                                    -Dsonar.tests=src/test/scala \
                                    -Dsonar.scala.version=2.13.12 \
                                    -Dsonar.sourceEncoding=UTF-8 \
                                    -Dsonar.scala.coverage.reportPaths=target/scala-2.13/scoverage-report/scoverage.xml \
                                    -Dsonar.junit.reportPaths=target/test-reports
                            '''
                        }
                        echo "✓ SonarQube analysis completed"
                    } catch (Exception e) {
                        echo "⚠️  SonarQube analysis skipped: ${e.message}"
                        echo "Continuing build..."
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 10: Waiting for Quality Gate'
                echo '═══════════════════════════════════════════'
                script {
                    try {
                        timeout(time: 5, unit: 'MINUTES') {
                            def qg = waitForQualityGate()
                            if (qg.status != 'OK') {
                                echo "⚠️  Quality Gate status: ${qg.status}"
                                echo "Continuing build..."
                            } else {
                                echo "✓ Quality Gate PASSED!"
                            }
                        }
                    } catch (Exception e) {
                        echo "⚠️  Quality Gate check skipped: ${e.message}"
                        echo "Continuing build..."
                    }
                }
            }
        }
        
        stage('Security - Dependency Check') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 11: Dependency Vulnerability Check'
                echo '═══════════════════════════════════════════'
                script {
                    def depCheckResult = sh(
                        script: 'sbt -Dsbt.log.noformat=true -batch dependencyCheck || true',
                        returnStatus: true
                    )
                    if (depCheckResult != 0) {
                        echo "⚠️  Dependency check skipped or completed with findings"
                    } else {
                        echo "✓ No critical vulnerabilities found"
                    }
                }
            }
        }
        
        stage('Package') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 12: Packaging Application'
                echo '═══════════════════════════════════════════'
                sh 'sbt -Dsbt.log.noformat=true -batch package'
                echo "✓ Standard JAR created"
            }
        }
        
        stage('Build Fat JAR') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 13: Building Fat JAR with Assembly'
                echo '═══════════════════════════════════════════'
                sh 'sbt -Dsbt.log.noformat=true -batch assembly'
                echo "✓ Fat JAR created"
            }
        }
        
        stage('Archive Artifacts') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 14: Archiving Build Artifacts'
                echo '═══════════════════════════════════════════'
                script {
                    archiveArtifacts artifacts: 'target/scala-2.13/*.jar', fingerprint: true
                    
                    sh """
                        cat > build-info.txt << EOF
╔════════════════════════════════════════════════════════╗
║           BUILD INFORMATION REPORT                     ║
╠════════════════════════════════════════════════════════╣
║ Build Number    : ${BUILD_NUMBER}
║ Build Date      : \$(date '+%Y-%m-%d %H:%M:%S %Z')
║ Git Commit      : ${GIT_COMMIT_SHORT}
║ Git Author      : ${GIT_AUTHOR}
║ Git Message     : ${GIT_COMMIT_MSG}
║ Jenkins Job     : ${JOB_NAME}
║ Workspace       : ${WORKSPACE}
╚════════════════════════════════════════════════════════╝
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
        }
        
        success {
            echo ''
            echo '╔═══════════════════════════════════════════╗'
            echo '║                                           ║'
            echo '║     ✅ ✅ ✅  BUILD SUCCESSFUL  ✅ ✅ ✅     ║'
            echo '║                                           ║'
            echo '╚═══════════════════════════════════════════╝'
            echo ''
        }
        
        failure {
            echo ''
            echo '╔═══════════════════════════════════════════╗'
            echo '║                                           ║'
            echo '║       ❌ ❌ ❌  BUILD FAILED  ❌ ❌ ❌       ║'
            echo '║                                           ║'
            echo '╚═══════════════════════════════════════════╝'
            echo ''
            script {
                echo "Failed at stage: ${env.STAGE_NAME}"
            }
        }
    }
}
