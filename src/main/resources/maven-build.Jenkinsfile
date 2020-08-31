stage('Build [Maven-Artifact]') {

    steps {

        script {

            if(env.BRANCH_NAME == 'master') {

                sh 'mvn install -Dmaven.test.skip=true -DskipTests=true -P prod -nsu $MAVEN_ARGS'

            } else {

                sh 'mvn install -Dmaven.test.skip=true -DskipTests=true -P prod $MAVEN_ARGS'
            }
        }
    }
}
