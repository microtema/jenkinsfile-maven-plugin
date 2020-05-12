stage('Initialize') {

    steps {
        script {
            dir('bootstrap') {
                try {
                    git branch: env.BRANCH_NAME, url: env.BOOTSTRAP_URL, credentialsId: 'SCM_CREDENTIALS'
                } catch (e) {
                    sh "echo unable to find branch! ${e}  retry with develop branch..."
                    git branch: 'develop', url: @BOOTSTRAP_URL@, credentialsId: 'SCM_CREDENTIALS'
                }
            }
            pipelineUtils = load './bootstrap/jenkins/pipeline-utils.groovy'
        }
    }
}
