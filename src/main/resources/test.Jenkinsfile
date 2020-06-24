stage('Test') {
    parallel {
        stage('Unit Tests') {
            steps {
                retry(2) {
                    sh 'mvn test $MAVEN_ARGS'
                }
            }

            post {
                always {
                    junit '**/*Test.xml'
                }
            }
        }

        stage('Integration Tests') {

            steps {
                retry(2) {
                    sh 'mvn test-compile failsafe:integration-test $MAVEN_ARGS'
                }
            }

            post {
                always {
                    junit '**/*IT.xml'
                }
            }
        }
    }
}
