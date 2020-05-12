    stage('Sonar Reports') {
        steps {
            sh 'mvn sonar:sonar -Dsonar.branch.name=$BRANCH_NAME $MAVEN_ARGS'
        }
    }
