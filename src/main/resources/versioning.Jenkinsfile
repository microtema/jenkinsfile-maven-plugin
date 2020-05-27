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
