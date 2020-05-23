script {
                if (env.BOOTSTRAP_URL.toLowerCase() == env.GIT_URL.toLowerCase()) {
                    env.MAVEN_ARGS = '-s ./settings.xml'
                } else {
                    dir('bootstrap') {
                        try {
                            git branch: env.BRANCH_NAME, url: env.BOOTSTRAP_URL, credentialsId: 'SCM_CREDENTIALS'
                        } catch (e) {
                            git branch: 'develop', url: env.BOOTSTRAP_URL, credentialsId: 'SCM_CREDENTIALS'
                        }
                        env.MAVEN_ARGS = '-s ./bootstrap/settings.xml'
                    }
                }
           }
