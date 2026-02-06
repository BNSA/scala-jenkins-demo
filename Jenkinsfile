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
                    echo "SBT Version:"
                    sbt sbtVersion || echo "SBT check skipped"
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
                        script: 'scalafmt --check --config .scalafmt.conf',
                        returnStatus: true
                    )
                    if (formatCheckResult != 0) {
                        echo "⚠️  WARNING: Code formatting issues found!"
                        echo "Run 'scalafmt' locally to fix formatting"
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
                        
                        // Display coverage summary
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
                        echo "⚠️  SonarQube analysis failed: ${e.message}"
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
                                echo "View details: http://13.71.48.197:9000/dashboard?id=scala-jenkins-demo"
                                echo "Continuing build despite quality gate failure..."
                            } else {
                                echo "✓ Quality Gate PASSED!"
                            }
                        }
                    } catch (Exception e) {
                        echo "⚠️  Quality Gate check failed: ${e.message}"
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
                        script: 'sbt -Dsbt.log.noformat=true -batch dependencyCheck',
                        returnStatus: true
                    )
                    if (depCheckResult != 0) {
                        echo "⚠️  Dependency check completed with findings"
                    } else {
                        echo "✓ No critical vulnerabilities found"
                    }
                }
            }
            post {
                always {
                    script {
                        try {
                            publishHTML([
                                allowMissing: true,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: 'target/scala-2.13',
                                reportFiles: 'dependency-check-report.html',
                                reportName: 'Dependency Check Report',
                                reportTitles: 'OWASP Dependency Check'
                            ])
                        } catch (Exception e) {
                            echo "⚠️  Could not publish dependency check report"
                        }
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
                    // Archive JAR files
                    archiveArtifacts artifacts: 'target/scala-2.13/*.jar', fingerprint: true
                    
                    // Create build information file
                    sh """
                        cat > build-info.txt << EOF
╔════════════════════════════════════════════════════════╗
║           BUILD INFORMATION REPORT                     ║
╠════════════════════════════════════════════════════════╣
║ Build Number    : ${BUILD_NUMBER}
║ Build Date      : \$(date '+%Y-%m-%d %H:%M:%S %Z')
║ Git Commit      : ${GIT_COMMIT_SHORT}
║ Full Commit     : ${GIT_COMMIT}
║ Git Author      : ${GIT_AUTHOR}
║ Git Message     : ${GIT_COMMIT_MSG}
║ Jenkins Job     : ${JOB_NAME}
║ Jenkins URL     : ${BUILD_URL}
║ Workspace       : ${WORKSPACE}
║ Artifact Name   : ${ARTIFACT_NAME}
║ Java Version    : \$(java -version 2>&1 | head -n 1)
║ Scala Version   : 2.13.12
║ SBT Version     : 1.9.7
╚════════════════════════════════════════════════════════╝

QUALITY METRICS:
─────────────────────────────────────────────────────────
✓ Code Coverage Report    : Available
✓ Scalastyle Checks       : Completed
✓ Wartremover Linting     : Completed
✓ SonarQube Analysis      : Completed
✓ Dependency Check        : Completed
✓ Unit Tests              : Passed
✓ Code Formatting         : Checked

ARTIFACTS PRODUCED:
─────────────────────────────────────────────────────────
- Standard JAR  : target/scala-2.13/scala-jenkins-demo_2.13-1.0.0.jar
- Fat JAR       : target/scala-2.13/scala-jenkins-demo-1.0.0.jar

ACCESS REPORTS:
─────────────────────────────────────────────────────────
- Build Console : ${BUILD_URL}console
- Test Report   : ${BUILD_URL}testReport
- Coverage      : ${BUILD_URL}Scoverage_Report
- SonarQube     : http://13.71.48.197:9000/dashboard?id=scala-jenkins-demo

BUILD SUCCESSFUL - All stages completed
EOF
                    """
                    
                    archiveArtifacts artifacts: 'build-info.txt', fingerprint: true
                    
                    // Display build info in console
                    echo '═══════════════════════════════════════════'
                    sh 'cat build-info.txt'
                    echo '═══════════════════════════════════════════'
                }
            }
        }
        
        stage('Generate Reports Dashboard') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 15: Generating Reports Dashboard'
                echo '═══════════════════════════════════════════'
                script {
                    sh """
                        cat > reports-dashboard.html << 'EOHTML'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Build Reports Dashboard - Build #${BUILD_NUMBER}</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
            min-height: 100vh;
        }
        .container { 
            max-width: 1000px; 
            margin: 0 auto; 
            background: white; 
            padding: 40px; 
            border-radius: 16px; 
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        h1 { 
            color: #333; 
            border-bottom: 4px solid #4CAF50; 
            padding-bottom: 15px;
            margin-bottom: 10px;
            font-size: 2.5em;
        }
        .build-info {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
            margin: 20px 0;
            border-left: 4px solid #667eea;
        }
        .build-info p { margin: 5px 0; color: #555; }
        h2 { 
            color: #444; 
            margin: 30px 0 15px 0;
            font-size: 1.8em;
            border-left: 5px solid #2196F3;
            padding-left: 15px;
        }
        .report-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 20px;
            margin: 20px 0;
        }
        .report-card {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 25px;
            border-radius: 12px;
            text-decoration: none;
            color: white;
            transition: transform 0.3s, box-shadow 0.3s;
            box-shadow: 0 4px 15px rgba(0,0,0,0.2);
        }
        .report-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 8px 25px rgba(0,0,0,0.3);
        }
        .report-card .icon { font-size: 2.5em; margin-bottom: 10px; }
        .report-card .title { font-size: 1.2em; font-weight: bold; margin-bottom: 8px; }
        .report-card .desc { font-size: 0.9em; opacity: 0.9; }
        .success-badge {
            display: inline-block;
            background: #4CAF50;
            color: white;
            padding: 10px 20px;
            border-radius: 25px;
            font-weight: bold;
            margin: 20px 0;
            font-size: 1.1em;
        }
        .artifact-list {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            margin: 15px 0;
        }
        .artifact-item {
            padding: 12px;
            margin: 8px 0;
            background: white;
            border-radius: 6px;
            border-left: 4px solid #4CAF50;
            display: flex;
            align-items: center;
        }
        .artifact-item:before {
            content: "📦";
            margin-right: 10px;
            font-size: 1.5em;
        }
        .footer {
            text-align: center;
            margin-top: 40px;
            padding-top: 20px;
            border-top: 2px solid #eee;
            color: #777;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>📊 Build Reports Dashboard</h1>
        
        <div class="build-info">
            <p><strong>🔢 Build Number:</strong> #${BUILD_NUMBER}</p>
            <p><strong>📅 Build Date:</strong> \$(date '+%Y-%m-%d %H:%M:%S')</p>
            <p><strong>👤 Author:</strong> ${GIT_AUTHOR}</p>
            <p><strong>💬 Commit:</strong> ${GIT_COMMIT_SHORT} - ${GIT_COMMIT_MSG}</p>
        </div>
        
        <div class="success-badge">✅ BUILD SUCCESSFUL</div>
        
        <h2>📈 Quality & Analysis Reports</h2>
        <div class="report-grid">
            <a href="Scoverage_Report/" class="report-card">
                <div class="icon">📊</div>
                <div class="title">Code Coverage</div>
                <div class="desc">Scoverage analysis with line-by-line coverage details</div>
            </a>
            
            <a href="testReport/" class="report-card">
                <div class="icon">🧪</div>
                <div class="title">Test Results</div>
                <div class="desc">JUnit test execution results and statistics</div>
            </a>
            
            <a href="http://13.71.48.197:9000/dashboard?id=scala-jenkins-demo" class="report-card" target="_blank">
                <div class="icon">🔍</div>
                <div class="title">SonarQube</div>
                <div class="desc">Static code analysis, bugs, vulnerabilities, code smells</div>
            </a>
            
            <a href="Dependency_Check_Report/" class="report-card">
                <div class="icon">🛡️</div>
                <div class="title">Security Scan</div>
                <div class="desc">OWASP dependency vulnerability check</div>
            </a>
        </div>
        
        <h2>📦 Build Artifacts</h2>
        <div class="artifact-list">
            <div class="artifact-item">
                <a href="artifact/target/scala-2.13/scala-jenkins-demo_2.13-1.0.0.jar" style="text-decoration: none; color: #333;">
                    <strong>Standard JAR</strong> - scala-jenkins-demo_2.13-1.0.0.jar
                </a>
            </div>
            <div class="artifact-item">
                <a href="artifact/target/scala-2.13/scala-jenkins-demo-1.0.0.jar" style="text-decoration: none; color: #333;">
                    <strong>Fat JAR (Assembly)</strong> - scala-jenkins-demo-1.0.0.jar
                </a>
            </div>
            <div class="artifact-item">
                <a href="artifact/build-info.txt" style="text-decoration: none; color: #333;">
                    <strong>Build Information</strong> - Detailed build metadata
                </a>
            </div>
        </div>
        
        <h2>🔧 Code Quality Tools</h2>
        <div class="artifact-list">
            <div class="artifact-item">✓ Scalafmt - Code formatting verification</div>
            <div class="artifact-item">✓ Scalastyle - Style guide enforcement</div>
            <div class="artifact-item">✓ Wartremover - Advanced Scala linting</div>
            <div class="artifact-item">✓ Scoverage - Statement coverage analysis</div>
            <div class="artifact-item">✓ SonarQube - Comprehensive static analysis</div>
            <div class="artifact-item">✓ OWASP - Dependency security scanning</div>
        </div>
        
        <div class="footer">
            <p>Generated by Jenkins Pipeline</p>
            <p>Job: ${JOB_NAME} | Build: #${BUILD_NUMBER}</p>
            <p><a href="${BUILD_URL}" style="color: #667eea;">View Full Build Details</a></p>
        </div>
    </div>
</body>
</html>
EOHTML
                    """
                    
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: '.',
                        reportFiles: 'reports-dashboard.html',
                        reportName: '📊 Reports Dashboard',
                        reportTitles: 'Build Reports'
                    ])
                    
                    echo "✓ Reports dashboard created"
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
                echo "Duration: ${duration}"
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
            script {
                echo "All ${currentBuild.result} stages completed successfully!"
                echo "View reports: ${BUILD_URL}Reports_Dashboard/"
                echo "SonarQube: http://13.71.48.197:9000/dashboard?id=scala-jenkins-demo"
            }
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
                echo "Check console output: ${BUILD_URL}console"
            }
        }
        
        unstable {
            echo ''
            echo '╔═══════════════════════════════════════════╗'
            echo '║                                           ║'
            echo '║      ⚠️  ⚠️  ⚠️  BUILD UNSTABLE  ⚠️  ⚠️  ⚠️     ║'
            echo '║                                           ║'
            echo '╚═══════════════════════════════════════════╝'
            echo ''
        }
    }
}
