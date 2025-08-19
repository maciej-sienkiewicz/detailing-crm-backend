pipeline {
    agent {
        docker {
            image 'gradle:jdk21-ubi'
            label 'docker'
            reuseNode true
            args '''
        --user 1000:1000
        -v gradle-cache:/home/gradle/.gradle
        -v gradle-tmp:/home/gradle/tmp
      '''
        }
    }

    environment {
        GRADLE_USER_HOME = '/home/gradle/.gradle'
        JAVA_TOOL_OPTIONS = '-Djava.io.tmpdir=/home/gradle/tmp'
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
