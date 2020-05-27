stage('Test') {
    parallel {
        stage('Unit Tests') {
            steps {
                sh 'mvn test $MAVEN_ARGS'
            }

            post {
                always {
                    junit '**/*Test.xml'
                }
            }
        }

        stage('Integration Tests') {

            steps {
                sh 'mvn test-compile failsafe:integration-test $MAVEN_ARGS'
            }

            post {
                always {
                    junit '**/*IT.xml'
                }
            }
        }
    }
}
