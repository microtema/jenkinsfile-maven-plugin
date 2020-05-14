    stage('Deployment') {

        when {
            environment name: 'DEPLOYABLE', value: 'true'
        }

        parallel {
            stage('ETU (feature-*)') {
                when {
                    branch 'feature-*'
                }
                steps {
                    withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                        withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-etu"]) {
                            createBranchIfNotExistsAndCommitChanges()
                        }
                    }
                }
            }

            stage('ETU (develop)') {
                when {
                    branch 'develop'
                }
                steps {
                    withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                        withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-etu"]) {
                            createAndMergeOpsRepoMergeRequest()
                        }
                    }
                }
            }

            stage('ETU (release-*)') {
                when {
                    branch 'release-*'
                }
                steps {
                    withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                        withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}"]) {
                            createBranchIfNotExistsAndCommitChanges()
                        }
                    }
                }
            }

            stage('ITU (master)') {
                when {
                    branch 'master'
                }
                steps {
                    withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                        withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-itu"]) {
                            createAndMergeOpsRepoMergeRequest()
                        }
                    }
                }
            }

            stage('SATU (master)') {
                when {
                    branch 'master'
                }
                steps {
                    withCredentials([usernamePassword(credentialsId: 'SCM_CREDENTIALS', usernameVariable: 'SCM_USERNAME', passwordVariable: 'SCM_PASSWORD')]) {
                        withEnv(["OPS_REPOSITORY_NAME=${env.BASE_NAMESPACE}-satu"]) {
                            createAndMergeOpsRepoMergeRequest()
                        }
                    }
                }
            }
        }
    }
