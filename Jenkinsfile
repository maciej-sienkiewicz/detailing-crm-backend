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
                label 'docker'
            }
            steps {
                sh 'docker build -f ./deploy/Dockerfile -t 172.17.0.1:5000/detailing-crm-backend:latest .'
                sh 'docker push 172.17.0.1:5000/detailing-crm-backend:latest'
            }
        }
    }


}
