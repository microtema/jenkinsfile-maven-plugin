# jenkinsfile generator
Reducing Boilerplate Code with jenkinnsfile maven plugin
> More Time for Feature and functionality
  Through a simple set of jenkinsfile templates and saving 60% of development time 

## Key Features
* Auto generate by maven compile phase
* Auto JUnit Tests detector by adding "JUnit Tests" stage
* Auto Integration Tests detector by adding "Integration Tests" stage
* Auto Dockerfile detector by adding "Build Docker" stage
* Auto Maven artifact detector by adding "Deploy Maven Artifact" stage
* Auto Sonar report detector by adding "Sonar Report" stage
* Auto Deployment to Cloud Platform by adding "Deployment" stage


## How to use

```
<properties>
    <!-- Jenkinsfile properties -->
    <jenkinsfile.app-name>${project.artifactId}</jenkinsfile.app-name>
    ...
</properties>
<plugins>
    <plugin>
        <groupId>de.microtema</groupId>
        <artifactId>jenkinsfile-maven-plugin</artifactId>
        <version>2.0.3</version>
        <configuration>
            <appName>${jenkinsfile.app-name}</appName>
            ...
        </configuration>
        <executions>
            <execution>
                <id>jenkinsfile</id>
                <phase>compile</phase>
                <goals>
                    <goal>generate</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
</plugins>
```

## Output 
> Jenkinsfile 
> NOTE: This is an example file.
```
pipeline {

    agent {
        label 'mvn'
    }

    environment {
        APP = ''
        BASE_NAMESPACE = ''

        VERSION = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout $MAVEN_ARGS', returnStdout: true).trim()
        CURRENT_TIME = sh(script: 'date +%Y-%m-%d-%H-%M', returnStdout: true).trim()
        CHANGE_AUTHOR_EMAIL = sh(script: 'git --no-pager show -s --format=\'%ae\'', returnStdout: true).trim()

        BOOTSTRAP_URL = 'https://github.com/microtema/bootstrap.git'
        MAVEN_ARGS = '-s ./bootstrap/settings.xml'
    }

    options {
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
    }

    triggers {
        upstream(upstreamProjects: "parent-project", threshold: hudson.model.Result.SUCCESS)
    }

    stages {
        
          stage('Initialize') {
      
              steps {
                  script {
                      dir('bootstrap') {
                          try {
                              git branch: env.BRANCH_NAME, url: env.BOOTSTRAP_URL, credentialsId: 'SCM_CREDENTIALS'
                          } catch (e) {
                              sh "echo unable to find branch! ${e}  retry with develop branch..."
                              git branch: 'develop', url: env.BOOTSTRAP_URL, credentialsId: 'SCM_CREDENTIALS'
                          }
                      }
                      pipelineUtils = load './bootstrap/jenkins/pipeline-utils.groovy'
                  }
      
                  sh 'whoami'
                  sh 'oc whoami'
                  sh 'mvn -version'
                  sh 'echo commit-id: $GIT_COMMIT'
                  sh 'echo change author: $CHANGE_AUTHOR_EMAIL'
      
                  sh 'echo project version: $VERSION'
                  sh 'echo current time: $CURRENT_TIME'
              }
          }

          stage('Versioning') {
      
              when {
                  anyOf {
                      branch 'release-*'
                      branch 'bugfix-*'
                  }
              }
      
              steps {
                  sh 'mvn release:update-versions -DdevelopmentVersion=2.1.0-SNAPSHOT $MAVEN_ARGS'
                  sh 'mvn versions:set -DnewVersion=$VERSION-$CURRENT_TIME-$BUILD_NUMBER $MAVEN_ARGS'
              }
          }

          stage('Compile') {
              steps {
                  script {
                      sh 'mvn compile -U $MAVEN_ARGS'
                  }
              }
          }

          stage('Test') {
              steps {
                  sh 'mvn test $MAVEN_ARGS'
              }
      
              post {
                  always {
                      junit '**/*Test.xml'
                  }
              }
          }

          stage('Sonar Reports') {
              steps {
                  sh 'mvn sonar:sonar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS'
              }
          }

          stage('Build [Maven-Artifact]') {
              steps {
                 sh 'mvn install -Dmaven.test.skip=true -DskipTests=true $MAVEN_ARGS'
              }
          }

          stage('Security Check') {
              steps {
                  sh 'mvn dependency-check:help -P security $MAVEN_ARGS'
              }
          }

          stage('Build [Docker-Image]') {
      
              when {
                  anyOf {
                      branch 'develop'
                      branch 'feature-*'
                      branch 'release-*'
                      branch 'master'
                      environment name: 'DEPLOYABLE', value: 'true'
                  }
              }
      
              steps {
                  buildDockerImage semVer: true
              }
          }

          stage('Deployment') {
      
              when {
                  environment name: 'DEPLOYABLE', value: 'true'
              }
      
              parallel {
                  stage('Develop') {
                      when {
                          branch 'develop'
                      }
                      steps {
                          withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                              withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-dev"]) {
                                  deployToDevelopStage()
                              }
                          }
                      }
                  }
      
                  stage('Pre Release)') {
                      when {
                          branch 'release-*'
                      }
                      steps {
                          withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                              withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-release-*"]) {
                                  deployToReleaseStage()
                              }
                          }
                      }
                  }
      
                  stage('Feature') {
                      when {
                          branch 'feature-*'
                      }
                      steps {
                          withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                              withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-feature-*"]) {
                                  deployToFeatureStage()
                              }
                          }
                      }
                  }
      
                  stage('Master') {
                      when {
                          branch 'master'
                      }
                      steps {
                          withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                              withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-prod"]) {
                                  deployToProdStage()
                              }
                          }
                      }
                  }
              }
          }

          stage('Aqua Reports') {
      
              environment {
                  AQUA_PROJECT_ID = '5'
                  AQUA_PRODUCT_ID = 'foo'
                  AQUA_RELEASE = '2.0.0'
                  AQUA_LEVEL = 'Release 2.0.0'
                  AQUA_JUNIT_TEST_FOLDER_ID = '1000'
                  AQUA_INTEGRATION_TEST_FOLDER_ID = '10001'
              }
      
              steps {
      
                  script {
      
                      def sendToAqua = { file, folderId, testType ->
      
                          def response = sh(script: """
                          curl -X POST \
                          -H "X-commit: ${env.GIT_COMMIT}" \
                          --data-binary @${file.path} \
                          "http://aqua.com/stream/"
                          """, returnStdout: true)
      
                          if (response != 'OK') {
                              error "Unable to report ${file.path} test in aqua ${folderId} folder!"
                          }
                      }
      
                      def reports = findFiles(glob: "**/*Test.xml")
                      reports.each { sendToAqua(it, env.JUNIT_TEST_AQUA_FOLDER_ID, 'Komponententest') }
      
                      reports = findFiles(glob: "**/*IT.xml")
                      reports.each { sendToAqua(it, env.INTEGRATION_TEST_AQUA_FOLDER_ID, 'Integrationstest') }
                  }
              }
          }

          stage('Promote to PROD?') {
      
              when {
                  branch 'master'
              }
      
              steps {
                  script {
                      try {
                          timeout(time: 1, unit: 'HOURS') {
                              input id: "promote-prod", message: 'Promote release to Prod?'
                          }
                      } catch (e) {
                          currentBuild.result = 'SUCCESS'
                          env.ABORTED = true
                          sh "Stopping early..."
                      }
                  }
              }
          }

          stage('Deployment [AWS]') {
      
              when {
                  allOf {
                      branch 'master'
                      expression {
                          env.ABORTED != 'true'
                      }
                  }
              }
      
              parallel {
      
                  stage('AWS (eu-central-1)') {
                      steps {
                          deployToStage opsRepositoryName: 'foo'
                      }
                  }
                  stage('AWS (us-central-1)') {
                      steps {
                          deployToStage opsRepositoryName: 'nf-bar'
                      }
                  }
      
              }
          }
    }

    post {

        always {
            script {
                if (currentBuild.result == null) {
                    currentBuild.result = 'SUCCESS'
                }
            }
        }

        failure {
            mail subject: "FAILED: Job '${env.JOB_NAME} in Branch ${env.BRANCH_NAME} [${env.BUILD_NUMBER}]'",
                    body: "FAILED: Job '${env.JOB_NAME} in Branch ${env.BRANCH_NAME} [${env.BUILD_NUMBER}]': Check console output at ${env.BUILD_URL}",
                    to: env.CHANGE_AUTHOR_EMAIL
        }
    }
}
```
    
## Technology Stack

* Java 1.8
    * Streams 
    * Lambdas
* Third Party Libraries
    * Commons-BeanUtils (Apache License)
    * Commons-IO (Apache License)
    * Commons-Lang3 (Apache License)
    * Junit (EPL 1.0 License)
* Code-Analyses
    * Sonar
    * Jacoco
    
## Test Coverage threshold
> 95%
    
## License

MIT (unless noted otherwise)

## Quality Gate Status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mtema_jenkinsfile-maven-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=mtema_jenkinsfile-maven-plugin)

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=mtema_jenkinsfile-maven-plugin&metric=coverage)](https://sonarcloud.io/dashboard?id=mtema_jenkinsfile-maven-plugin)

[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=mtema_jenkinsfile-maven-plugin&metric=sqale_index)](https://sonarcloud.io/dashboard?id=mtema_jenkinsfile-maven-plugin)
