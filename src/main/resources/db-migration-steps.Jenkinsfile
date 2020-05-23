steps {
                sh 'mvn flyway:migrate -P @STAGE_NAME@ -Doracle.jdbc.fanEnabled=false $MAVEN_ARGS'
            }
