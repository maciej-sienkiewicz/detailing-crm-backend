pipeline {
    agent {
        docker {
            image 'gradle:jdk21-ubi'
            label 'docker'
            reuseNode true
            args '-v $HOME/.gradle:/home/gradle/.gradle'
        }
    }


    stages {
        stage('Build') {
            steps {
                sh 'chmod +x gradlew || true'
                sh './gradlew build'          
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
