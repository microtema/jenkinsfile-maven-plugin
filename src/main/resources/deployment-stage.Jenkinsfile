stage(@STAGE_DISPLAY_NAME@) {
    environment {
        STAGE_NAME = @STAGE_NAME@
        BRANCH_PATTERN = @BRANCH_PATTERN@
    }
    when {
        allOf {
            branch env.BRANCH_PATTERN
            environment name: 'DEPLOYABLE', value: 'true'
        }
    }
    steps {
        withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
            withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-${env.STAGE_NAME}"]) {
                @DEPLOY_FUNCTION@
            }
        }
    }
}
