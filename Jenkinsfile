pipeline {
    agent {
        docker {
            image 'gradle:jdk21-ubi'
            label 'docker'
            reuseNode true
            args '--user 1000:1000 -v gradle-cache:/home/gradle/.gradle'
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
