pipeline {
    agent any
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }
    
    environment {
        SBT_OPTS = '-Xmx2g -XX:+UseG1GC'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 1: Checking out source code'
                echo '═══════════════════════════════════════════'
                checkout scm
                script {
                    env.GIT_COMMIT_SHORT = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                    env.GIT_AUTHOR = sh(returnStdout: true, script: 'git log -1 --pretty=%an').trim()
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
                sh 'sbt -Dsbt.log.noformat=true -batch scalafmtAll scalafmtSbt'
                echo '✓ Code automatically formatted'
            }
        }
        
        stage('Compile') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 4: Compiling Scala Code'
                echo '═══════════════════════════════════════════'
                sh 'sbt -Dsbt.log.noformat=true -batch clean compile Test/compile'
                echo '✓ Compilation successful'
            }
        }
        
        stage('Unit Tests') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 5: Running Unit Tests'
                echo '═══════════════════════════════════════════'
                sh '''
                    sbt -Dsbt.log.noformat=true -batch \
                    "testOnly *Spec -- -h target/test-reports/unit -u target/test-reports/unit-junit"
                '''
                echo '✓ Unit tests completed'
            }
            post {
                always {
                    // Publish JUnit test results
                    junit allowEmptyResults: true, testResults: 'target/test-reports/unit-junit/*.xml'
                    
                    // Publish HTML test reports
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/test-reports/unit',
                        reportFiles: 'index.html',
                        reportName: 'Unit Test Report',
                        reportTitles: 'Unit Test Results'
                    ])
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 6: Running Integration Tests'
                echo '═══════════════════════════════════════════'
                sh '''
                    sbt -Dsbt.log.noformat=true -batch \
                    "it:testOnly *IntegrationSpec -- -h target/test-reports/integration -u target/test-reports/integration-junit"
                '''
                echo '✓ Integration tests completed'
            }
            post {
                always {
                    // Publish JUnit test results
                    junit allowEmptyResults: true, testResults: 'target/test-reports/integration-junit/*.xml'
                    
                    // Publish HTML test reports
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/test-reports/integration',
                        reportFiles: 'index.html',
                        reportName: 'Integration Test Report',
                        reportTitles: 'Integration Test Results'
                    ])
                }
            }
        }
        
        stage('Code Coverage') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 7: Generating Code Coverage Report'
                echo '═══════════════════════════════════════════'
                sh 'sbt -Dsbt.log.noformat=true -batch clean coverage test coverageReport'
                echo '✓ Coverage report generated'
            }
            post {
                always {
                    // Publish coverage report
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/scala-2.13/scoverage-report',
                        reportFiles: 'index.html',
                        reportName: 'Code Coverage Report',
                        reportTitles: 'Scoverage Report'
                    ])
                    
                    // Archive coverage data
                    archiveArtifacts artifacts: 'target/scala-2.13/scoverage-report/**/*', allowEmptyArchive: true
                }
            }
        }
        
        stage('Package JAR') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 8: Packaging Application JAR'
                echo '═══════════════════════════════════════════'
                sh 'sbt -Dsbt.log.noformat=true -batch package'
                echo '✓ JAR packaged'
            }
        }
        
        stage('Build Fat JAR') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 9: Building Fat JAR with Assembly'
                echo '═══════════════════════════════════════════'
                sh 'sbt -Dsbt.log.noformat=true -batch assembly'
                echo '✓ Fat JAR created'
            }
        }
        
        stage('Archive Artifacts') {
            steps {
                echo '═══════════════════════════════════════════'
                echo '  Stage 10: Archiving Build Artifacts'
                echo '═══════════════════════════════════════════'
                archiveArtifacts artifacts: 'target/scala-2.13/*.jar', fingerprint: true
                echo '✓ Artifacts archived'
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
                
                // Count test results
                def testResults = junit(testResults: 'target/test-reports/**/*.xml', allowEmptyResults: true)
                def totalTests = testResults.totalCount
                def passedTests = testResults.totalCount - testResults.failCount
                def failedTests = testResults.failCount
                
                echo "Total Tests: ${totalTests}"
                echo "Passed: ${passedTests}"
                echo "Failed: ${failedTests}"
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
            echo 'BUILD SUCCESSFUL ✅'
            echo ''
            echo '📊 Reports Available:'
            echo '   • Unit Test Report (HTML)'
            echo '   • Integration Test Report (HTML)'
            echo '   • Code Coverage Report (HTML)'
            echo '   • JUnit XML Reports'
            echo ''
            echo 'Click on "Unit Test Report" or "Integration Test Report" in the sidebar to view!'
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
                if (currentBuild.result == 'FAILURE') {
                    echo "Failed at stage: ${env.STAGE_NAME}"
                }
            }
        }
    }
}
