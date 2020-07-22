stage('Pull Request [PROD]') {

    when {
        allOf {
            branch 'master'
            environment name: 'DEPLOYABLE', value: 'true'
            expression {
                env.ABORTED != 'true'
            }
        }
    }

    parallel {
@STAGES@
    }
}
