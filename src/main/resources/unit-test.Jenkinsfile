    stage('Test') {
        steps {
            sh 'mvn test $MAVEN_ARGS'
        }

        post {
            always {
                junit 'target/surefire-reports/**/*Test.xml'
            }
        }
    }
