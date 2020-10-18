stage(@STAGE_DISPLAY_NAME@) {

    environment {
        STAGE_NAME = @STAGE_NAME@
        NAMESPACE = "${env.BASE_NAMESPACE}-${env.STAGE_NAME}"
    }

    options {
        timeout(time: 10, unit: 'MINUTES')
    }

    when {
        branch @BRANCH_PATTERN@
    }

    steps {

        script {
             Throwable caughtException = null

             try {
                 catchError(buildResult: 'SUCCESS', stageResult: 'ABORTED') {
                     waitForReadiness()
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
