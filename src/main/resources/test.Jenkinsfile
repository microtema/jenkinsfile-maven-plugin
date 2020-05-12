stage('Test') {
    parallel {
        stage('Unit Tests') {
            steps {
                sh 'mvn test $MAVEN_ARGS'
            }

            post {
                always {
                    junit 'target/surefire-reports/**/*Test.xml'
                }
            }
        }

        stage('Integration Tests') {

            steps {
                sh 'mvn test-compile failsafe:integration-test $MAVEN_ARGS'
            }

            post {
                always {
                    junit 'target/failsafe-reports/**/*IT.xml'
                }
            }
        }
    }
}
