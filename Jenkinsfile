pipeline {

    agent {
        label 'mvn8'
    }

    environment {

        CURRENT_TIME = sh(script: 'date +%Y-%m-%d-%H-%M', returnStdout: true).trim()
        CHANGE_AUTHOR_EMAIL = sh(script: "git --no-pager show -s --format='%ae'", returnStdout: true).trim()


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
      
              environment {
                  BOOTSTRAP_URL = ''
              }
      
              steps {
                  
                  sh 'whoami'
                  sh 'oc whoami'
                  sh 'mvn -version'
                  sh 'echo commit-id: $GIT_COMMIT'
                  sh 'echo change author: $CHANGE_AUTHOR_EMAIL'
              }
          }

          stage('Versioning') {
      
              environment {
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
                  sh 'mvn compile -U $MAVEN_ARGS'
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

          stage('Tag [Release]') {
      
              environment{
                  VERSION = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout $MAVEN_ARGS', returnStdout: true).trim()
              }
      
              when {
                  branch 'master'
              }
      
              steps {
                  script {
                      withGit {
                          try {
                              sh 'git tag $VERSION $GIT_COMMIT'
                              sh 'git push origin $VERSION'
                          } catch (e) {
      
                              if (env.BRANCH_NAME != 'master') {
                                  throw e
                              }
      
                              sh 'echo there is already a tag for this version $VERSION'
                          }
                      }
                  }
              }
          }

          stage('Publish [Maven-Artifact]') {
              steps {
                  script {
                      try {
                           sh 'mvn deploy -Dmaven.test.skip=true -DskipTests=true $MAVEN_ARGS'
                      } catch (e) {
      
                          if (env.BRANCH_NAME != 'master') {
                              throw e
                          }
      
                          sh 'echo there is already a publication for this version $VERSION'
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
                      to:  sh(script: "git --no-pager show -s --format='%ae'", returnStdout: true).trim()
        }
    }
}

