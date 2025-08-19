pipeline {
    agent {
        BladeRunner {
            image 'gradle:jdk21-ubi'
            label 'docker' // lub 'Blade_runner' – jeśli nie masz innego labela
            reuseNode true
        }
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
            }
        }
    }

    post {
        always {
            script {
                node {
                    cleanWs()
                }
            }
        }
    }
}
