stage('Test') {

    steps {
        sh 'mvn test-compile failsafe:integration-test $MAVEN_ARGS'
    }

    post {
        always {
            junit 'target/failsafe-reports/**/*IT.xml'
        }
    }
}
