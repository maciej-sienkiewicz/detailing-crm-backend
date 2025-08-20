pipeline {
    agent {
        docker {
            image 'gradle:7.6.6-jdk17-corretto'
            label 'docker'
            reuseNode true
        }
    }

    environment {
        GRADLE_USER_HOME = '/home/gradle/.gradle'
    }
    stages {
        stage('Build') {
            steps {
                sh 'mkdir -p "$GRADLE_USER_HOME"'
                sh 'chmod +x gradlew || true'
                sh './gradlew -g "$GRADLE_USER_HOME" build'
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
