script {
                dir('bootstrap') {
                    try {
                        def remoteUrl = sh(script: 'git remote -v', returnStdout:true).trim()
                        if (remoteUrl.contains(env.BOOTSTRAP_URL)) {
                            env.MAVEN_ARGS = '-s ./settings.xml'
                        } else {
                            git branch: env.BRANCH_NAME, url: env.BOOTSTRAP_URL, credentialsId: 'SCM_CREDENTIALS'
                        }
                    } catch (e) {
                        git branch: 'develop', url: env.BOOTSTRAP_URL, credentialsId: 'SCM_CREDENTIALS'
                    }
                }
           }
