stage('Performance Tests') {

    when {
        environment name: 'DEPLOYABLE', value: 'true'
    }

    parallel {
@STAGES@
    }
}
