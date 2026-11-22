pipeline {
 agent any
 tools { jdk 'jdk17'; maven 'maven3' }
 options { skipDefaultCheckout(); timestamps(); disableConcurrentBuilds() }
 parameters {
  booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Analyze with the existing SonarQube')
  booleanParam(name: 'PUBLISH_DOCKERHUB', defaultValue: false, description: 'Publish six service images to Docker Hub')
  booleanParam(name: 'PUBLISH_NEXUS', defaultValue: false, description: 'Publish Maven artifacts to the existing Nexus')
 }
 environment {
  SONAR_HOST_URL = 'http://sonarqube:9000'
  NEXUS_URL = 'http://nexus:8081'
 }
 stages {
  stage('Checkout') { steps { checkout scm } }
  stage('Build and test') { steps { sh 'mvn -B -ntp clean install' } }
  stage('SonarQube') {
   when { expression { params.RUN_SONAR } }
   steps {
    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
     sh 'mvn -B -ntp org.sonarsource.scanner.maven:sonar-maven-plugin:3.11.0.3922:sonar -Dsonar.projectKey=fst-events -Dsonar.qualitygate.wait=true'
    }
   }
  }
  stage('Publish to Nexus') {
   when { expression { params.PUBLISH_NEXUS } }
   steps {
    withCredentials([usernamePassword(credentialsId: 'nexus-credentials', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASSWORD')]) {
     sh 'mvn -B -ntp -s ops/maven/settings.example.xml deploy -DskipTests'
    }
   }
  }
  stage('Publish images to Docker Hub') {
   when { expression { params.PUBLISH_DOCKERHUB } }
   steps {
    withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DH_USER', passwordVariable: 'DH_TOKEN')]) {
     sh '''
      set -eu
      set +x
      export DOCKER_CONFIG
      DOCKER_CONFIG="$(mktemp -d)"
      trap 'rm -rf "$DOCKER_CONFIG"' EXIT
      printf '%s' "$DH_TOKEN" | docker login --username "$DH_USER" --password-stdin

      tag="build-${BUILD_NUMBER}-$(git rev-parse --short HEAD)"
      for service in config-server eureka-server api-gateway participant-service logistics-service event-service; do
        image="$DH_USER/fst-events-$service:$tag"
        docker build -f ops/Dockerfile --build-arg "SERVICE=$service" -t "$image" .
        docker push "$image"
      done
     '''
    }
   }
  }
 }
 post {
  always {
   junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
   archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/*.jar,**/target/site/jacoco/**'
  }
 }
}
