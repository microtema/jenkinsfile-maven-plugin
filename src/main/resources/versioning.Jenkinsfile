stage('Versioning') {

    environment {
        VERSION = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout $MAVEN_ARGS', returnStdout: true).trim()
        NEXT_VERSION = nextVersion(VERSION)
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

            if(env.BRANCH_NAME == 'master') {

                sh 'mvn release:update-versions -DdevelopmentVersion=0.0.1-SNAPSHOT $MAVEN_ARGS'
                sh 'mvn versions:set -DnewVersion=$NEXT_VERSION $MAVEN_ARGS'

            } else {

                sh 'mvn release:update-versions -DdevelopmentVersion=0.0.1-SNAPSHOT $MAVEN_ARGS'
                sh "mvn versions:set -DnewVersion=${env.VERSION.replaceAll('-SNAPSHOT','')}-${env.CURRENT_TIME}-${env.BUILD_NUMBER} ${env.MAVEN_ARGS}"
            }
        }
    }
}
