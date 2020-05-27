    stage('Security Check') {
        steps {
            sh 'mvn dependency-check:help -P security $MAVEN_ARGS'
        }
    }
