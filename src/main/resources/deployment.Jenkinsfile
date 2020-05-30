stage('Deployment') {

    when {
        environment name: 'DEPLOYABLE', value: 'true'
    }

    parallel {
@STAGES@
    }
}
