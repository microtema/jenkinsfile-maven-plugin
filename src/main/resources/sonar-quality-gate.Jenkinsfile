stage('Sonar Quality Gate') {

    environment {
        SONAR_TOKEN = @SONAR_TOKEN@
    }

    options {
        timeout(time: 10, unit: 'MINUTES')
    }

    steps {

        script {
             Throwable caughtException = null

             try {
                 catchError(buildResult: 'SUCCESS', stageResult: 'ABORTED') {
                     checkSonarQualityGate()
                 }
             } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
                 error "Caught ${e.toString()}"
             } catch (Throwable e) {
                 caughtException = e
             }

             if (caughtException) {
                 error caughtException.message
             }
        }
    }
}
