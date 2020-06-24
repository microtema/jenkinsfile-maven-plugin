stage('Versioning') {

    environment {
        VERSION = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout $MAVEN_ARGS', returnStdout: true).trim()
    }

    when {
        anyOf {
            branch 'release/*'
            branch 'release-*'
            branch 'hotfix/*'
            branch 'hotfix-*'
            branch 'bugfix/*'
            branch 'bugfix-*'
            branch 'master'
        }
    }

    steps {

        script {

            if(env.BRANCH_NAME != 'master') {

                sh 'mvn release:update-versions -DdevelopmentVersion=2.1.0-SNAPSHOT $MAVEN_ARGS'
                sh 'mvn versions:set -DnewVersion=$VERSION-$CURRENT_TIME-$BUILD_NUMBER $MAVEN_ARGS'

            } else if(env.BRANCH_NAME.contains('SNAPSHOT')) {

                sh 'mvn release:update-versions -DdevelopmentVersion=$BRANCH_NAME $MAVEN_ARGS'
                sh "mvn versions:set -DnewVersion=${env.BRANCH_NAME.replaceAll('-SNAPSHOT','')} ${env.MAVEN_ARGS}"
            }
        }
    }
}
