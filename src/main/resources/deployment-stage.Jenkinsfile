stage(@STAGE_DISPLAY_NAME@) {
    environment {
        STAGE_NAME = @STAGE_NAME@
    }
    when {
        branch @BRANCH_PATTERN@
    }
    steps {
        withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
            withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-${env.STAGE_NAME}"]) {
                @DEPLOY_FUNCTION@
            }
        }
    }
}
