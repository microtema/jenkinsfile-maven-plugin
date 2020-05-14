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

            stage('RC') {
                when {
                    branch 'release-*'
                }
                steps {
                    buildDockerImage semVer: true
                }
            }

            stage('Master') {
                when {
                    branch 'master'
                }
                steps {
                    buildDockerImage semVer: true
                }
            }
        }
    }

