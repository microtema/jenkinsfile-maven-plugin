environment {
    APP = @APP@
    VERSION = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout $MAVEN_ARGS', returnStdout: true).trim()
    CURRENT_TIME = sh(script: 'date +%Y-%m-%d-%H-%M', returnStdout: true).trim()
    CHANGE_AUTHOR_EMAIL = sh(script: 'git --no-pager show -s --format=\'%ae\'', returnStdout: true).trim()

    BASE_NAMESPACE = @BASE_NAMESPACE@
    // NOTE: Since we use the same Jenkinsfile in multiple CI-Servers [cbp-*, cbp-blueprints-*, authentication-maintenance-*]
    //       we need a conditional deployment, only on different  namespaces
    DEPLOYABLE = sh(script: 'oc whoami', returnStdout: true).trim().startsWith("system:serviceaccount:${env.BASE_NAMESPACE}")

    MAVEN_ARGS = '-s ./bootstrap/settings.xml'

    sh 'whoami'
    sh 'oc whoami'
    sh 'mvn -version'
    sh 'echo commit-id: $GIT_COMMIT'
    sh 'echo change author: $CHANGE_AUTHOR_EMAIL'

    sh 'echo project version: $VERSION'
    sh 'echo current time: $CURRENT_TIME'
    sh 'echo deployable: $DEPLOYABLE'
}
