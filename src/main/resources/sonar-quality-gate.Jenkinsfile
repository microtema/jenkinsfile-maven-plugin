stage('Sonar Quality Gate') {

    environment {
        SONAR_TOKEN = @SONAR_TOKEN@
    }

    steps {

        script {

            def properties = readProperties file: './target/sonar/report-task.txt'
            def ceTaskUrl = properties.get('ceTaskUrl')
            def dashboardUrl = properties.get('dashboardUrl')

            def waitForStatus = {

                def response = sh(script: "curl -u ${env.SONAR_TOKEN}: ${ceTaskUrl}", returnStdout: true).trim()
                def responseJson = new groovy.json.JsonSlurper().parseText(response)
                def status = responseJson.task.status

                switch (status) {
                    case 'SUCCESS': return true
                    case 'FAILED':
                        currentBuild.result = 'FAILURE'
                        echo "Sonar Quality Gate failed in ${env.BRANCH_NAME}. See dashboard: ${dashboardUrl}"
                        throw new Exception("Quality not passed in ${env.BRANCH_NAME}!")
                    default: return false
                }
            }

            while (!waitForStatus.call()) {
                echo 'Sonar Quality Gate is not available or in progress! Retry after few seconds...'
                sleep time: 30, unit: 'SECONDS'
            }

            echo "Sonar Quality Gate passed. See dashboard: ${dashboardUrl}"
        }
    }
}
