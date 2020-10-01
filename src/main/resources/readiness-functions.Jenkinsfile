def waitForReadiness(url, closure) {

    if(url) {

        def waitForReadinessImpl = {
            try {
                def response = httpRequest url
                echo "@Get ${url} -> ${response.content}"
                def json = new groovy.json.JsonSlurper().parseText(response.content)
                closure.call(json)
            } catch (e) {
                false
            }
        }

        while (!waitForReadinessImpl.call()) {
            echo 'Application is not available or not ready! Retry after few seconds...'
            sleep(time: 30, unit: "SECONDS")
        }

        echo 'Application is ready and updated'

    } else {

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
}
