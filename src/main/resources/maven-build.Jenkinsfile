    stage('Build [Maven-Artifact]') {
        steps {
           sh 'mvn install -Dmaven.test.skip=true -DskipTests=true $MAVEN_ARGS'
        }
    }
