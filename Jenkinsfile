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
                sh '''
          docker run --rm \
            -v "$PWD:/workspace" \
            -w /workspace \
            node:24.12.0-alpine3.23 \
            sh -c "npm ci && npm test"
        '''
            }
        }
        stage('Docker Build & Push') {
            agent {
                label 'docker'
            }
            steps {
                script {
                    def branch = env.GIT_BRANCH ?: 'unknown'
                    def tag

                    if (branch == 'origin/main') {
                        tag = 'latest'
                    } else if (branch == 'origin/develop') {
                        tag = 'develop'
                    } else {
                        error("Build przerwany: branch '${branch}' nie jest obsługiwany (tylko 'main' lub 'develop').")
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
