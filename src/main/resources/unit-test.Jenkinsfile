stage('Test') {
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
