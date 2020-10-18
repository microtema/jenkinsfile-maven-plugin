def waitForReadiness(url, closure) {

    def waitForReadinessImpl = {
        try {
            def response = httpRequest url
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
}
