environment {
        APP = @APP@
        BASE_NAMESPACE = @BASE_NAMESPACE@

        CURRENT_TIME = sh(script: 'date +%Y-%m-%d-%H-%M', returnStdout: true).trim()
        CHANGE_AUTHOR_EMAIL = sh(script: 'git --no-pager show -s --format=\'%ae\'', returnStdout: true).trim()

        // NOTE: Since we use the same Jenkinsfile in multiple CI-Servers [cbp-*, cbp-blueprints-*, authentication-maintenance-*]
        //       we need a conditional deployment, only on different  namespaces
        DEPLOYABLE = sh(script: 'oc whoami', returnStdout: true).trim().startsWith("system:serviceaccount:${env.BASE_NAMESPACE}")

        BOOTSTRAP_URL = @BOOTSTRAP_URL@
        MAVEN_ARGS = '-s ./bootstrap/settings.xml'
        CBP_KEYSTORE_LOCATION = '../bootstrap/keystore'
    }
