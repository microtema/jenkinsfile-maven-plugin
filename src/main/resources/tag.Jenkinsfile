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
