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
                sh "echo ${e.toString()}"
            }
        }
    }
}
