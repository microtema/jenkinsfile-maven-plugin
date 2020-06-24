stage(@STAGE_DISPLAY_NAME@) {

    environment {
        MAVEN_PROFILE = @MAVEN_PROFILE@
    }

    when {
        branch @BRANCH_PATTERN@
    }

    steps {
        sh 'mvn flyway:migrate -P $MAVEN_PROFILE -Doracle.jdbc.fanEnabled=false $MAVEN_ARGS'
    }
}
