node {
	// agent none 
      environment { 
          NAME = "registry.gitlab.com/idtrust.id/billtrust"
          VERSION = "${env.BUILD_ID}-${env.GIT_COMMIT}"
          IMAGE = "${NAME}:${VERSION}"
          IMAGE_REPO = "registry.gitlab.com"
      }
      stage('Clone repository') { 
      	checkout scm
      } 


    stage('SonarQube Analysis') {
    def mvn = tool 'Default Maven';
    withSonarQubeEnv() {
      sh "${mvn}/bin/mvn clean verify sonar:sonar -Dsonar.projectKey=idtrust.id_billtrust_AYnykwsDUSjBEeFimSOu"
    }
    }
      
    stage('mvn Install') {
      docker.image('maven:3.8.7-openjdk-18').inside {
        script {
            sh "mvn clean install -Dmaven.test.skip" // 'mvnw' command (e.g. "./mvnw deploy")
        }
      }
    }


    stage('Build') {
        dockerImage = docker.build("registry.gitlab.com/idtrust.id/billtrust")
    }

    stage('Push image') {
        withDockerRegistry([ credentialsId: "git", url: "https://registry.gitlab.com" ]) {
        dockerImage.push("${env.BRANCH_NAME}-${env.BUILD_NUMBER}")
        }
    }
    
    stage('Trigger Billing-eks') {
          echo "triggering billing-eks job"
          build job: 'billing-eks', parameters: [string(name: 'DOCKERTAG', value: env.BUILD_NUMBER)]
}    

}
