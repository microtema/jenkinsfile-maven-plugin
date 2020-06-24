stage('Promote to PROD?') {

    when {
        allOf {
            branch 'master'
            environment name: 'DEPLOYABLE', value: 'true'
        }
    }

    steps {
        script {
            try {
                timeout(time: 1, unit: 'HOURS') {
                    input id: 'promote-prod', message: 'Promote release to Prod?'
                }
            } catch (e) {
                currentBuild.result = 'SUCCESS'
                env.ABORTED = true
                sh 'echo Stopping early...'
            }
        }
    }
}
