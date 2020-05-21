pipeline {

    agent {
        label 'mvn8'
    }

    environment {

        CURRENT_TIME = sh(script: 'date +%Y-%m-%d-%H-%M', returnStdout: true).trim()
        CHANGE_AUTHOR_EMAIL = sh(script: 'git --no-pager show -s --format=\'%ae\'', returnStdout: true).trim()

        APP = ''
        BASE_NAMESPACE = ''
        DEPLOYABLE = sh(script: 'oc whoami', returnStdout: true).trim().startsWith("system:serviceaccount:${env.BASE_NAMESPACE}")

    }

    options {
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
    }

    triggers {
        upstream(upstreamProjects: "", threshold: hudson.model.Result.SUCCESS)
    }

    stages {
        
          stage('Initialize') {
      
              steps {
                  
                  sh 'whoami'
                  sh 'oc whoami'
                  sh 'mvn -version'
                  sh 'echo commit-id: $GIT_COMMIT'
                  sh 'echo change author: $CHANGE_AUTHOR_EMAIL'
              }
          }

          stage('Versioning') {
      
              environment{
                  VERSION = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout $MAVEN_ARGS', returnStdout: true).trim()
              }
      
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

          stage('Build [Maven-Artifact]') {
              steps {
                 sh 'mvn install -Dmaven.test.skip=true -DskipTests=true -P prod $MAVEN_ARGS'
              }
          }

          stage('Security Check') {
              steps {
                  sh 'mvn dependency-check:help -P security $MAVEN_ARGS'
              }
          }

          stage('Build [Docker-Image]') {
      
              when {
                  environment name: 'DEPLOYABLE', value: 'true'
              }
      
              parallel {
                  stage('Feature') {
                      when {
                          branch 'feature-*'
                      }
                      steps {
                          buildDockerImage semVer: true
                      }
                  }
      
                  stage('Develop') {
                      when {
                          branch 'develop'
                      }
                      steps {
                          buildDockerImage semVer: true
                      }
                  }
      
                  stage('RC') {
                      when {
                          branch 'release-*'
                      }
                      steps {
                          buildDockerImage semVer: true
                      }
                  }
      
                  stage('Master') {
                      when {
                          branch 'master'
                      }
                      steps {
                          buildDockerImage semVer: true
                      }
                  }
              }
          }
      

          stage('Deployment') {
      
              when {
                  environment name: 'DEPLOYABLE', value: 'true'
              }
      
              parallel {
                  stage('ETU (feature-*)') {
                      when {
                          branch 'feature-*'
                      }
                      steps {
                          withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                              withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-etu"]) {
                                  createBranchIfNotExistsAndCommitChanges()
                              }
                          }
                      }
                  }
      
                  stage('ETU (develop)') {
                      when {
                          branch 'develop'
                      }
                      steps {
                          withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                              withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-etu"]) {
                                  createAndMergeOpsRepoMergeRequest()
                              }
                          }
                      }
                  }
      
                  stage('ETU (release-*)') {
                      when {
                          branch 'release-*'
                      }
                      steps {
                          withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                              withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}"]) {
                                  createBranchIfNotExistsAndCommitChanges()
                              }
                          }
                      }
                  }
      
                  stage('ITU (master)') {
                      when {
                          branch 'master'
                      }
                      steps {
                          withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                              withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-itu"]) {
                                  createAndMergeOpsRepoMergeRequest()
                              }
                          }
                      }
                  }
      
                  stage('SATU (master)') {
                      when {
                          branch 'master'
                      }
                      steps {
                          withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                              withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-satu"]) {
                                  createAndMergeOpsRepoMergeRequest()
                              }
                          }
                      }
                  }
              }
          }

          stage('Aqua Reports') {
      
              environment {
                  AQUA_URL = ''
                  AQUA_PROJECT_ID = ''
                  AQUA_PRODUCT_ID = ''
                  AQUA_RELEASE = ''
                  AQUA_LEVEL = ''
                  AQUA_JUNIT_TEST_FOLDER_ID = ''
                  AQUA_INTEGRATION_TEST_FOLDER_ID = ''
              }
      
              steps {
      
                  script {
      
                      def sendToAqua = { file, folderId, testType ->
      
                          def response = sh(script: """
                          curl -X POST \
                          -H "X-aprojectid: ${env.AQUA_PROJECT_ID}" \
                          -H "X-afolderid: ${folderId}" \
                          -H "X-aprodukt: ${env.AQUA_PRODUCT_ID}" \
                          -H "X-aausbringung: ${env.AQUA_RELEASE}" \
                          -H "X-astufe: ${env.AQUA_LEVEL}" \
                          -H "X-ateststufe: ${testType}" \
                          -H "X-commit: ${env.GIT_COMMIT}" \
                          --data-binary @${file.path} \
                          "${env.AQUA_URL}"
                          """, returnStdout: true)
      
                          if (response != 'OK') {
                              error "Unable to report ${file.path} test in aqua ${folderId} folder!"
                          }
                      }
      
                      def reports = findFiles(glob: "**/*Test.xml")
                      reports.each { sendToAqua(it, env.AQUA_JUNIT_TEST_FOLDER_ID, 'Komponententest') }
      
                      reports = findFiles(glob: "**/*IT.xml")
                      reports.each { sendToAqua(it, env.AQUA_INTEGRATION_TEST_FOLDER_ID, 'Integrationstest') }
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

          stage('Pull Request [PROD]') {
      
              when {
                  allOf {
                      branch 'master'
                      expression {
                          env.ABORTED != 'true'
                      }
                  }
              }
      
              parallel {
      
                  stage('FOO') {
                      steps {
                          createOpsRepoMergeRequest opsRepositoryName: 'foo'
                      }
                  }
                  stage('BAR') {
                      steps {
                          createOpsRepoMergeRequest opsRepositoryName: 'nf-bar'
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

