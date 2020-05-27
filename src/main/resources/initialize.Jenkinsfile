stage('Initialize') {

    environment {
        BOOTSTRAP_URL = @BOOTSTRAP_URL@
    }

    steps {
@BOOTSTRAP@
        sh 'whoami'
        sh 'oc whoami'
        sh 'mvn -version'
        sh 'echo commit-id: $GIT_COMMIT'
        sh 'echo change author: $CHANGE_AUTHOR_EMAIL'
    }
}
