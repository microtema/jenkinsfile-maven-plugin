    stage('Readiness Probe') {

        when {
            environment name: 'DEPLOYABLE', value: 'true'
        }

        parallel {
            @STAGES@
        }
    }
