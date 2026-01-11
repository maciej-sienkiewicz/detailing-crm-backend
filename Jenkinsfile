pipeline {
    agent none

    environment {
        GRADLE_USER_HOME = '/home/gradle/.gradle'
        IMAGE_NAME = '172.17.0.1:5000/detailing-crm-backend'
    }

    stages {

        stage('Build') {
            agent { label 'docker' }

            steps {
                script {
                    docker.image('gradle:9.2.1-jdk17-ubi10').inside(
                        "-v ${GRADLE_USER_HOME}:${GRADLE_USER_HOME}"
                    ) {
                        sh 'chmod +x gradlew || true'
                        sh './gradlew build'
                    }
                }
            }
        }

        stage('Docker Build & Push') {
            agent { label 'docker' }

            steps {
                script {
                    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'unknown'
                    def tag

                    if (branch == 'main' || branch == 'origin/main') {
                        tag = 'latest'
                    } else if (branch == 'develop' || branch == 'origin/develop') {
                        tag = 'develop'
                    } else {
                        error("Build przerwany: branch '${branch}' nie jest obsługiwany (tylko main / develop).")
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
