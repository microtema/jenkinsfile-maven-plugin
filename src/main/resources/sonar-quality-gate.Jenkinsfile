stage('Sonar Quality Gate') {

    environment {
        SONAR_TOKEN = @SONAR_TOKEN@
    }

    options {
        timeout(time: 3, unit: 'MINUTES')
    }

    steps {
        catchError(buildResult: 'SUCCESS', stageResult: 'ABORTED') {
            script {
                checkSonarQualityGate()
            }
        }
    }
}
