stage(@STAGE_DISPLAY_NAME@) {

    environment {
        STAGE_NAME = @STAGE_NAME@
        NAMESPACE = "${env.BASE_NAMESPACE}-${env.STAGE_NAME}"
    }

    options {
        timeout(time: 10, unit: 'MINUTES')
    }

    when {
        branch @BRANCH_PATTERN@
    }

    steps {

        script {
             waitForReadiness(@ENDPOINT@, @CLOSURE@)
        }
    }
}
