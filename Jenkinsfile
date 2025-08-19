pipeline {
    agent {
        docker {
            image 'maven:3.8.6-openjdk-17'
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
