pipeline {
    agent none

    environment {
        IMAGE_NAME = '172.17.0.1:5000/detailing-crm-backend'
    }

    stages {
        stage('Build') {
            agent {
                label 'docker'
            }
            steps {
                script {
                    docker.image('gradle:7.6.6-jdk17-corretto').inside(
                        '-v /home/gradle/.gradle:/home/gradle/.gradle'
                    ) {
                        sh 'chmod +x gradlew || true'
                        sh './gradlew build'
                    }
                }
            }
        }

        stage('Docker Build & Push') {
            agent {
                label 'docker'
            }
            steps {
                script {
                    def branch = env.BRANCH_NAME
                    def tag

                    if (branch == 'main') {
                        tag = 'latest'
                    } else if (branch == 'develop') {
                        tag = 'develop'
                    } else {
                        error("Build przerwany: branch '${branch}' nie jest obsługiwany.")
                    }

                    sh """
                      docker build -f ./deploy/Dockerfile -t ${IMAGE_NAME}:${tag} .
                      docker push ${IMAGE_NAME}:${tag}
                    """
                }
            }
        }
    }
}
