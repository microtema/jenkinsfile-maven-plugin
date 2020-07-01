stage(@STAGE_DISPLAY_NAME@) {

    environment {
        JOB_NAME = @JOB_NAME@
        STAGE_NAME = @STAGE_NAME@
        VERSION = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout $MAVEN_ARGS', returnStdout: true).trim()
    }

    when {
        branch @BRANCH_PATTERN@
    }

    steps {

        script {
            triggerJob "../${env.JOB_NAME}/${env.BRANCH_NAME}"
        }
    }
}
