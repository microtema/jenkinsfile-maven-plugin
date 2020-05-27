steps {

    script {

        def namespace = "${env.BASE_NAMESPACE}-etu"

        def waitForPodReadinessImpl = {

            def pods = sh(script: "oc get pods --namespace ${namespace} | grep -E '${env.APP}.*' | grep -v build | grep -v deploy", returnStdout: true)
            .trim().split('\n')
            .collect { it.split(' ')[0] }

            pods.find {
                try {
                    sh(script: "oc describe pod ${it} --namespace ${namespace} | grep -c 'git-commit=${env.GIT_COMMIT}'", returnStdout: true).trim().toInteger()
                } catch (e) {
                    false
                }
            }
        }

        while (!waitForPodReadinessImpl.call()) {
            echo 'Pod is not available or not ready! Retry after few seconds...'
            sleep(time: 30, unit: "SECONDS")
        }

        echo "${pods}"
        echo 'Pod is ready and updated'
    }
}
