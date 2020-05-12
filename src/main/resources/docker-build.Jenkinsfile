stage('Build [Docker-Image]') {

    when {
        anyOf {
            branch 'develop'
            branch 'feature-*'
            branch 'release-*'
            branch 'master'
            environment name: 'DEPLOYABLE', value: 'true'
        }
    }

    steps {
        buildDockerImage semVer: true
    }
}
