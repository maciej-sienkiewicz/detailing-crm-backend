pipeline {
  agent {
    docker {
      image 'gradle:jdk21-ubi'
      args '-v $WORKSPACE/.gradle:/home/gradle/.gradle'
      reuseNode true
    }
  }

  environment {
    GRADLE_OPTS = '-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true -Dorg.gradle.jvmargs="-Xmx2g -Xms512m"'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build') {
      steps {
        sh '''
          set -euxo pipefail
          if [ -f ./gradlew ]; then
            chmod +x ./gradlew
            ./gradlew --no-daemon clean build
          else
            gradle --no-daemon clean build
          fi
        '''
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
          archiveArtifacts artifacts: '**/build/libs/*.jar', fingerprint: true
        }
      }
    }
  }

  post {
    always {
      script {
        cleanWs()
      }
    }
  }
}
