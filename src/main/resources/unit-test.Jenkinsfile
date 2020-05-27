stage('Test') {
    steps {
        sh 'mvn test $MAVEN_ARGS'
    }

    post {
        always {
            junit '**/*Test.xml'
        }
    }
}
