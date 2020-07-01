stage(@STAGE_DISPLAY_NAME@) {

    environment {
        STAGE_NAME = @STAGE_NAME@
        NAMESPACE = "${env.BASE_NAMESPACE}-${env.STAGE_NAME}"
    }

    when {
        branch @BRANCH_PATTERN@
    }

    steps {

        script {

            waitForReadiness()
        }
    }
}
