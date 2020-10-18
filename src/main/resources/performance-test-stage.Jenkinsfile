stage(@STAGE_DISPLAY_NAME@) {

    environment {
        MAVEN_PROFILE = @MAVEN_PROFILE@
    }

    when {
        branch @BRANCH_PATTERN@
    }

    steps {
        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
            sh 'mvn validate -P performance-$MAVEN_PROFILE $MAVEN_ARGS'
        }
    }

    post {
        always {
            script {
                publishJMeterReport()
            }
        }
    }
}
