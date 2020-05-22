    stage('Database Migration') {
        steps {
            sh 'mvn flyway:migrate -P ETU -Doracle.jdbc.fanEnabled=false $MAVEN_ARGS'
        }
    }
