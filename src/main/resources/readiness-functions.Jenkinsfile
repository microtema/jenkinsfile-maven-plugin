def waitForReadiness() {

    def podName
    def waitForPodReadinessImpl = {

        def pods = sh(script: "oc get pods --namespace ${env.NAMESPACE} | grep -v build | grep -v deploy | awk '/${env.APP}/ {print \$1}'", returnStdout: true)
        .trim().split('\n').findAll { it }

        pods.find {
            podName = it
            try {
                sh(script: "oc describe pod ${podName} --namespace ${env.NAMESPACE} | grep -c 'git-commit=${env.GIT_COMMIT}'", returnStdout: true).trim().toInteger()
            } catch (e) {
                false
            }
        }
    }

    while (!waitForPodReadinessImpl.call()) {
        echo 'Pod is not available or not ready! Retry after few seconds...'
        sleep time: 30, unit: 'SECONDS'
    }

    echo "Pod ${podName} is ready and updated"
}
