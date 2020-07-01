pipeline {

@AGENT@

@ENVIRONMENT@

@OPTIONS@

@TRIGGERS@

    stages {
@STAGES@
    }

    post {

        always {
            script {
                if (currentBuild.result == null) {
                    currentBuild.result = 'SUCCESS'
                }
            }
        }

        failure {
            mail subject: "FAILED: Job '${env.JOB_NAME} in Branch ${env.BRANCH_NAME} [${env.BUILD_NUMBER}]'",
                    body: "FAILED: Job '${env.JOB_NAME} in Branch ${env.BRANCH_NAME} [${env.BUILD_NUMBER}]': Check console output at ${env.BUILD_URL}",
                      to:  sh(script: "git --no-pager show -s --format='%ae'", returnStdout: true).trim()
        }
    }
}
@FUNCTIONS@
