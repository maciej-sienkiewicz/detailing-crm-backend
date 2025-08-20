pipeline {
    agent none


    environment {
        GRADLE_USER_HOME = '/home/gradle/.gradle'
    }
    stages {
        stage('Build') {
            agent {
                docker {
                    image 'gradle:7.6.6-jdk17-corretto'
                    label 'docker'
                    reuseNode true
                }
            }
            steps {
                sh 'mkdir -p "$GRADLE_USER_HOME"'
                sh 'chmod +x gradlew || true'
                sh './gradlew -g "$GRADLE_USER_HOME" build'
            }
        }

        stage('Docker Build & Push') {
            agent {
                image 'docker:27-cli'
                label 'docker'
            }
            steps {
                sh 'docker build -f ./deploy/Dockerfile -t registry:5000/myapp:latest .'
                sh 'docker push registry:5000/myapp:latest'
            }
        }
    }


    post {
        always {
            cleanWs()
        }
    }
}
