stage('Sonar Quality Gate') {

    environment {
        SONAR_TOKEN = @SONAR_TOKEN@
    }

    steps {

        script {

            def properties = readProperties file: './target/sonar/report-task.txt'
            def ceTaskUrl = properties.get('ceTaskUrl')
            def dashboardUrl = properties.get('dashboardUrl')
            def token = env.SONAR_TOKEN
            def script = "curl -u ${token}: ${ceTaskUrl}"

            def waitForStatus = {

                def response = sh(script: script, returnStdout: true).trim()
                def responseJson = new groovy.json.JsonSlurper().parseText(response)
                def status = responseJson.task.status

                switch (status) {
                    case 'SUCCESS': return true
                    case 'FAILED':
                        currentBuild.result = 'FAILURE'
                        echo "Sonar Quality Gate failed. See dashboard: ${dashboardUrl}"
                        throw new Exception("Quality : ${status} not passed!")
                    default: return false
                }
            }

            while (!waitForStatus.call()) {
                echo 'Sonar Quality Gate is not available or in progress! Retry after few seconds...'
                sleep(time: 30, unit: 'SECONDS')
            }

            echo "Sonar Quality Gate passed. See dashboard: ${dashboardUrl}"
        }
    }
}
