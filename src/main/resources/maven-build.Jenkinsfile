stage('Build [Maven-Artifact]') {
    steps {
       sh 'mvn install -Dmaven.test.skip=true -DskipTests=true -P prod $MAVEN_ARGS'
    }
}
