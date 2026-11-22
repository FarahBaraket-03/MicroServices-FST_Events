pipeline {
 agent any
 tools { jdk 'jdk17'; maven 'maven3' }
 options { timestamps(); disableConcurrentBuilds() }
 parameters {
  booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Analyze with the existing SonarQube')
  booleanParam(name: 'PUBLISH_NEXUS', defaultValue: false, description: 'Publish Maven artifacts to the existing Nexus')
 }
 environment {
  SONAR_HOST_URL = 'http://sonarqube:9000'
  NEXUS_URL = 'http://nexus:8081'
 }
 stages {
  stage('Checkout') { steps { checkout scm } }
  stage('Build and test') { steps { sh 'mvn -B -ntp clean verify' } }
  stage('SonarQube') {
   when { expression { params.RUN_SONAR } }
   steps {
    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
     sh 'mvn -B -ntp sonar:sonar -Dsonar.qualitygate.wait=true'
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
 }
 post {
  always {
   junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
   archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/*.jar,**/target/site/jacoco/**'
  }
 }
}
