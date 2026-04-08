// ============================================================
// NexaBank CI/CD Pipeline — Jenkins (Traditional Enterprise CI)
// Demonstrates: SDLC stages, parallel quality checks, approval
// gates, branch-conditional deployments (Agile/Waterfall hybrid)
// ============================================================
pipeline {
    agent any

    environment {
        JAVA_HOME       = '/usr/lib/jvm/java-17-openjdk'
        DOCKER_REGISTRY = 'nexabank'
        SONAR_HOST      = 'http://sonarqube:9000'
        BUILD_VERSION   = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'local'}"
    }

    options {
        timeout(time: 45, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                echo "Building branch: ${env.BRANCH_NAME}, version: ${BUILD_VERSION}"
            }
        }

        // ── Run static analysis in parallel (speeds up pipeline)
        stage('Code Quality') {
            parallel {
                stage('Checkstyle') {
                    steps {
                        sh 'mvn checkstyle:check -q'
                    }
                }
                stage('SpotBugs') {
                    steps {
                        sh 'mvn spotbugs:check -q'
                    }
                }
            }
        }

        stage('Unit Tests') {
            steps {
                sh '''
                    mvn test \
                        -pl services/account-service,services/transaction-service, \
                            services/notification-service,services/loan-service \
                        --no-transfer-progress
                '''
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java'
                    )
                }
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests --no-transfer-progress'
                archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonar') {
                    sh '''
                        mvn sonar:sonar \
                            -Dsonar.projectKey=nexabank-platform \
                            -Dsonar.host.url=${SONAR_HOST}
                    '''
                }
                // Block until quality gate result is available
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    def services = [
                        'infrastructure/eureka-server',
                        'infrastructure/config-server',
                        'infrastructure/api-gateway',
                        'services/account-service',
                        'services/transaction-service',
                        'services/notification-service',
                        'services/loan-service'
                    ]
                    services.each { svc ->
                        def imgName = svc.split('/')[1]
                        docker.build("${DOCKER_REGISTRY}/${imgName}:${BUILD_VERSION}", "./${svc}")
                        docker.build("${DOCKER_REGISTRY}/${imgName}:latest", "./${svc}")
                    }
                    // Python AI layer
                    docker.build("${DOCKER_REGISTRY}/ai-layer:${BUILD_VERSION}", './ai-layer')
                }
            }
        }

        // ── Auto-deploy to dev on every develop branch push
        stage('Deploy to Dev') {
            when { branch 'develop' }
            steps {
                sh 'docker-compose -f docker-compose.yml up -d --build'
                echo "Dev deployment complete. Notify offshore team in Confluence."
            }
        }

        stage('Integration Tests') {
            when { branch 'develop' }
            steps {
                sh 'mvn verify -P integration-test --no-transfer-progress'
            }
            post {
                always {
                    junit '**/target/failsafe-reports/*.xml'
                }
            }
        }

        // ── Manual approval gate before staging (Change Management)
        stage('Deploy to Staging') {
            when { branch 'main' }
            steps {
                input(
                    message: "Deploy version ${BUILD_VERSION} to Staging?",
                    ok: 'Approve',
                    submitter: 'tech-lead,release-manager'
                )
                sh 'docker-compose -f docker-compose.staging.yml up -d --build'
                echo "Staging deployment approved and complete."
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        failure {
            emailext(
                to: 'team@nexabank.com',
                subject: "BUILD FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    Pipeline failed on branch ${env.BRANCH_NAME}.
                    Build: ${env.BUILD_URL}
                    Stage: ${env.STAGE_NAME}
                    Onshore/offshore leads: please review and coordinate resolution.
                """
            )
        }
        success {
            echo "Pipeline complete for ${env.BRANCH_NAME}. Notify offshore team via Confluence."
        }
    }
}
