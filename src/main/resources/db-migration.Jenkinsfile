stage('DB Migration') {

    when {
        environment name: 'DEPLOYABLE', value: 'true'
    }

    parallel {
@STAGES@
    }
}
