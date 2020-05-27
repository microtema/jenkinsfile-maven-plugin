    stage('Build [Docker-Image]') {

        when {
            environment name: 'DEPLOYABLE', value: 'true'
        }

        parallel {
            stage('Feature') {
                when {
                    branch 'feature-*'
                }
                steps {
                    buildDockerImage semVer: true
                }
            }

            stage('Develop') {
                when {
                    branch 'develop'
                }
                steps {
                    buildDockerImage semVer: true
                }
            }

            stage('Pre-Release') {
                when {
                    branch 'release-*'
                }
                steps {
                    buildDockerImage semVer: true
                }
            }

            stage('Release') {
                when {
                    branch 'master'
                }
                steps {
                    buildDockerImage semVer: true
                }
            }
        }
    }

