stage('Build [Docker-Image]') {

    when {
         anyOf {
            branch 'feature-*'
            branch 'develop'
            branch 'release-*'
            branch 'hotfix-*'
            branch 'master'
            environment name: 'DEPLOYABLE', value: 'true'
         }
    }

    steps {
        buildDockerImage semVer: true
    }
}
