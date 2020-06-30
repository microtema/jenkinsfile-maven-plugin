stage(@STAGE_DISPLAY_NAME@) {

    environment {
        STAGE_NAME = @STAGE_NAME@
        NAMESPACE = "${env.BASE_NAMESPACE}-${env.STAGE_NAME}"
    }

    when {
        branch @BRANCH_PATTERN@
    }

    steps {

        script {

            def waitForPodReadinessImpl = {

                def pods = sh(script: 'oc get pods --namespace $NAMESPACE | grep -v build | grep -v deploy | awk \'/$APP/ {print $1}\'', returnStdout: true).trim().split('\n')

                pods.find {

                    env.POD_NAME = it

                    try {
                        sh(script: 'oc describe pod $POD_NAME --namespace $NAMESPACE | grep -c \'git-commit=$GIT_COMMIT\'', returnStdout: true).trim().toInteger()
                    } catch (e) {
                        false
                    }
                }
            }

            while (!waitForPodReadinessImpl.call()) {
                echo 'Pod is not available or not ready! Retry after few seconds...'
                sleep time: 30, unit: 'SECONDS'
            }

            echo "Pod ${env.POD_NAME} is ready and updated"
        }
    }
}
