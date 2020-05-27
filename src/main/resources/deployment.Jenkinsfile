stage('Deployment') {

    when {
        environment name: 'DEPLOYABLE', value: 'true'
    }

    parallel {
        stage('Feature') {
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

        stage('Develop') {
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

        stage('Release') {
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

        stage('ITU') {
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

        stage('SATU') {
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
