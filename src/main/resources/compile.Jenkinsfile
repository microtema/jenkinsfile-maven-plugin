stage('Compile') {

    steps {

        script {

            if(env.BRANCH_NAME == 'master') {

                sh 'mvn compile -nsu $MAVEN_ARGS'

            } else {

                sh 'mvn compile -U $MAVEN_ARGS'
            }
        }
    }
}
