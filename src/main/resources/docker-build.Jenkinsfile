stage('Build [Docker-Image]') {

    when {
        allOf {
            environment name: 'DEPLOYABLE', value: 'true'
            anyOf {
                branch 'feature-*'
                branch 'develop'
                branch 'release-*'
                branch 'hotfix-*'
                branch 'master'
            }
        }
    }

    steps {
        buildDockerImage semVer: true
    }
}
