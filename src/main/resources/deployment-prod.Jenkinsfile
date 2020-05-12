stage('Pull Request [PROD]') {

    when {
        allOf {
            branch 'master'
            expression {
                env.ABORTED != 'true'
            }
        }
    }

    parallel {
        @STAGES@
    }
}
