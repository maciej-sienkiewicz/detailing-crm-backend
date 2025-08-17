pipeline {
    agent any

    tools {
        jdk 'jdk-17'          // nazwa JDK skonfigurowana w Jenkinsie
        gradle 'gradle-7'     // nazwa Gradle skonfigurowana w Jenkinsie
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh "./gradlew clean build"
            }
        }
        
        stage('Test') {
            steps {
                sh "./gradlew test"
            }
            post {
                always {
                    junit 'build/test-results/test/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh "./gradlew bootJar"
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }
    }

    post {
        success {
            echo "✅ Build zakończony sukcesem!"
        }
        failure {
            echo "❌ Build nie powiódł się!"
        }
    }
}
