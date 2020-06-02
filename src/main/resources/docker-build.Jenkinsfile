stage('Build [Docker-Image]') {

    when {
        anyOf {
            branch 'feature-*'
            branch 'develop'
            branch 'release-*'
            branch 'hotfix-*'
            environment name: 'DEPLOYABLE', value: 'true'
        }
    }

    steps {

        script {

            if(env.BRANCH_NAME ==~ /(hotfix|release)-.+/){
                env.BRANCH_NAME = 'master'
            }

            buildDockerImage semVer: true
        }

    }
}
