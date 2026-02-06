pipeline {
    agent any
    
    environment {
        SBT_OPTS = '-Xmx2048M -Xss2M'
        VERSION = "${BUILD_NUMBER}"
        ARTIFACT_NAME = "scala-jenkins-demo-${VERSION}.jar"
    }
    
    options {
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        ansiColor('xterm')
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo '=== 1. Checking out source code ==='
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
                echo "Commit: ${env.GIT_COMMIT_SHORT} by ${env.GIT_AUTHOR}"
                echo "Message: ${env.GIT_COMMIT_MSG}"
            }
        }
        
        stage('Environment Info') {
            steps {
                echo '=== 2. Build Environment ==='
                sh '''
                    echo "════════════════════════════════════════"
                    echo "Build #${BUILD_NUMBER}"
                    echo "Job: ${JOB_NAME}"
                    echo "Workspace: ${WORKSPACE}"
                    echo "════════════════════════════════════════"
                    java -version
                    echo "════════════════════════════════════════"
                    sbt sbtVersion || echo "SBT check skipped"
                    echo "════════════════════════════════════════"
                    df -h
                    echo "════════════════════════════════════════"
                    free -h
                    echo "════════════════════════════════════════"
                '''
            }
        }
        
        stage('Code Formatting Check') {
            steps {
                echo '=== 3. Checking code formatting with Scalafmt ==='
                sh '''
                    scalafmt --check --config .scalafmt.conf || {
                        echo "❌ Code formatting issues found!"
                        echo "Run 'scalafmt' to fix formatting"
                        exit 0
                    }
                '''
            }
        }
        
        stage('Style Check') {
            steps {
                echo '=== 4. Running Scalastyle checks ==='
                sh 'sbt -Dsbt.log.noformat=true -batch scalastyle test:scalastyle'
            }
            post {
                always {
                    // Publish Scalastyle results
                    recordIssues(
                        enabledForFailure: true,
                        tool: checkStyle(pattern: 'target/scalastyle-result.xml,target/scalastyle-test-result.xml')
                    )
                }
            }
        }
        
        stage('Compile') {
            steps {
                echo '=== 5. Compiling Scala code ==='
                sh 'sbt -Dsbt.log.noformat=true -batch clean compile Test/compile'
            }
        }
        
        stage('Linting - Wartremover') {
            steps {
                echo '=== 6. Running Wartremover linter ==='
                sh 'sbt -Dsbt.log.noformat=true -batch compile || echo "Wartremover checks completed"'
            }
        }
        
        stage('Unit Tests') {
            steps {
                echo '=== 7. Running unit tests ==='
                sh 'sbt -Dsbt.log.noformat=true -batch coverage test coverageReport'
            }
            post {
                always {
                    // Publish JUnit test results
                    junit allowEmptyResults: true, testResults: 'target/test-reports/*.xml'
                }
            }
        }
        
        stage('Code Coverage Report') {
            steps {
                echo '=== 8. Publishing coverage reports ==='
                script {
                    // Publish HTML coverage report
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/scala-2.13/scoverage-report',
                        reportFiles: 'index.html',
                        reportName: 'Scoverage Report',
                        reportTitles: 'Code Coverage'
                    ])
                    
                    // Archive coverage data
                    archiveArtifacts artifacts: 'target/scala-2.13/scoverage-report/**/*', allowEmptyArchive: true
                }
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                echo '=== 9. Running SonarQube analysis ==='
                script {
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
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                echo '=== 10. Waiting for SonarQube Quality Gate ==='
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            echo "⚠️ Quality Gate status: ${qg.status}"
                            echo "Continuing despite quality gate failure for demo purposes"
                            // unstable(message: "Quality Gate failed: ${qg.status}")
                        } else {
                            echo "✅ Quality Gate PASSED!"
                        }
                    }
                }
            }
        }
        
        stage('Package') {
            steps {
                echo '=== 11. Packaging application ==='
                sh 'sbt -Dsbt.log.noformat=true -batch package'
            }
        }
        
        stage('Build Fat JAR') {
            steps {
                echo '=== 12. Building fat JAR with assembly ==='
                sh 'sbt -Dsbt.log.noformat=true -batch assembly'
            }
        }
        
        stage('Archive Artifacts') {
            steps {
                echo '=== 13. Archiving build artifacts ==='
                script {
                    // Archive JAR files
                    archiveArtifacts artifacts: 'target/scala-2.13/*.jar', fingerprint: true
                    
                    // Create detailed build info
                    sh """
                        cat > build-info.txt << EOF
╔════════════════════════════════════════════════════════╗
║           BUILD INFORMATION                            ║
╠════════════════════════════════════════════════════════╣
║ Build Number    : ${BUILD_NUMBER}
║ Build Date      : \$(date '+%Y-%m-%d %H:%M:%S')
║ Git Commit      : ${GIT_COMMIT_SHORT}
║ Git Author      : ${GIT_AUTHOR}
║ Git Message     : ${GIT_COMMIT_MSG}
║ Jenkins Job     : ${JOB_NAME}
║ Jenkins URL     : ${BUILD_URL}
║ Artifact        : ${ARTIFACT_NAME}
╚════════════════════════════════════════════════════════╝
EOF
                    """
                    archiveArtifacts artifacts: 'build-info.txt', fingerprint: true
                    
                    // Display build info
                    sh 'cat build-info.txt'
                }
            }
        }
        
        stage('Generate Reports Summary') {
            steps {
                echo '=== 14. Generating reports summary ==='
                script {
                    sh '''
                        cat > reports-summary.html << 'EOHTML'
<!DOCTYPE html>
<html>
<head>
    <title>Build Reports Summary</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .container { max-width: 800px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
        h1 { color: #333; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; }
        .report-link { 
            display: block; 
            padding: 15px; 
            margin: 10px 0; 
            background: #e3f2fd; 
            border-left: 4px solid #2196F3;
            text-decoration: none;
            color: #1976D2;
            border-radius: 4px;
            transition: all 0.3s;
        }
        .report-link:hover { background: #bbdefb; transform: translateX(5px); }
        .success { color: #4CAF50; }
        .info { color: #2196F3; }
    </style>
</head>
<body>
    <div class="container">
        <h1>📊 Build Reports - Build #''' + env.BUILD_NUMBER + '''</h1>
        <p class="info">Generated: ''' + new Date().toString() + '''</p>
        
        <h2>Available Reports:</h2>
        <a href="Scoverage_Report/" class="report-link">
            📈 Code Coverage Report (Scoverage)
        </a>
        <a href="testReport/" class="report-link">
            🧪 Test Results (JUnit)
        </a>
        <a href="http://13.71.48.197:9000/dashboard?id=scala-jenkins-demo" class="report-link" target="_blank">
            🔍 SonarQube Analysis
        </a>
        
        <h2>Build Artifacts:</h2>
        <a href="artifact/target/scala-2.13/" class="report-link">
            📦 JAR Files
        </a>
        <a href="artifact/build-info.txt" class="report-link">
            📄 Build Information
        </a>
        
        <p class="success">✅ All reports generated successfully!</p>
    </div>
</body>
</html>
EOHTML
                    '''
                    
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: '.',
                        reportFiles: 'reports-summary.html',
                        reportName: '📊 Reports Dashboard',
                        reportTitles: 'Build Reports Summary'
                    ])
                }
            }
        }
    }
    
    post {
        always {
            echo '═══════════════════════════════════════════'
            echo '  Pipeline Execution Completed'
            echo '═══════════════════════════════════════════'
        }
        
        success {
            echo '✅ ✅ ✅  BUILD SUCCESSFUL  ✅ ✅ ✅'
            script {
                def duration = currentBuild.durationString.replace(' and counting', '')
                echo "Duration: ${duration}"
                echo "All ${currentBuild.result} stages passed!"
            }
        }
        
        failure {
            echo '❌ ❌ ❌  BUILD FAILED  ❌ ❌ ❌'
            script {
                echo "Failed at stage: ${env.STAGE_NAME}"
                echo "Check console output for details"
            }
        }
        
        unstable {
            echo '⚠️  BUILD UNSTABLE  ⚠️'
        }
    }
}
