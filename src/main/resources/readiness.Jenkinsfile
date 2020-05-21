    stage('Readiness Probe') {

        when {
            environment name: 'DEPLOYABLE', value: 'true'
        }

        parallel {
            stage('ETU (feature-*)') {
                when {
                    branch 'feature-*'
                }
                steps {
                    script {

                        def namespace = "${env.BASE_NAMESPACE}-etu"

                        def waitForPodReadinessImpl {
                            def pods = sh(script: "oc get pods --namespace ${namespace} | grep -E '${env.APP}.*' | grep -v build | grep -v deploy", returnStdout: true).trim().split('\n')
                            echo "${pods}"

                            pods.find {
                                def count = sh(script: "oc describe pod ${it} --namespace ${namespace} | grep -c 'git-commit=${env.COMMIT_ID}'", returnStdout: true).trim()
                                echo "${count} -> ${env.GIT_COMMIT}"
                                count == '1'
                            } != null
                        }

                        while (!waitForPodReadinessImpl()) {
                            echo 'Pod is not available or not ready! Retry after few seconds...'
                            sleep(time: 30, unit: "SECONDS")
                        }

                        echo 'Pod is ready and updated'
                    }
                }
            }

            stage('ETU (develop)') {
                when {
                    branch 'develop'
                }
                steps {
                    script {

                        def namespace = "${env.BASE_NAMESPACE}-etu"

                        def waitForPodReadinessImpl {
                            def pods = sh(script: "oc get pods --namespace ${namespace} | grep -E '${env.APP}.*' | grep -v build | grep -v deploy", returnStdout: true).trim().split('\n')
                            echo "${pods}"

                            pods.find {
                                def count = sh(script: "oc describe pod ${it} --namespace ${namespace} | grep -c 'git-commit=${env.COMMIT_ID}'", returnStdout: true).trim()
                                echo "${count} -> ${env.GIT_COMMIT}"
                                count == '1'
                            } != null
                        }

                        while (!waitForPodReadinessImpl()) {
                            echo 'Pod is not available or not ready! Retry after few seconds...'
                            sleep(time: 30, unit: "SECONDS")
                        }

                        echo 'Pod is ready and updated'
                    }
                }
            }

            stage('ETU (release-*)') {
                when {
                    branch 'release-*'
                }
                steps {
                    script {

                        def namespace = "${env.BASE_NAMESPACE}-itu"

                        def waitForPodReadinessImpl {
                            def pods = sh(script: "oc get pods --namespace ${namespace} | grep -E '${env.APP}.*' | grep -v build | grep -v deploy", returnStdout: true).trim().split('\n')
                            echo "${pods}"

                            pods.find {
                                def count = sh(script: "oc describe pod ${it} --namespace ${namespace} | grep -c 'git-commit=${env.COMMIT_ID}'", returnStdout: true).trim()
                                echo "${count} -> ${env.GIT_COMMIT}"
                                count == '1'
                            } != null
                        }

                        while (!waitForPodReadinessImpl()) {
                            echo 'Pod is not available or not ready! Retry after few seconds...'
                            sleep(time: 30, unit: "SECONDS")
                        }

                        echo 'Pod is ready and updated'
                    }
                }
            }

            stage('ITU (master)') {
                when {
                    branch 'master'
                }
                steps {
                    script {

                        def namespace = "${env.BASE_NAMESPACE}-itu"

                        def waitForPodReadinessImpl {
                            def pods = sh(script: "oc get pods --namespace ${namespace} | grep -E '${env.APP}.*' | grep -v build | grep -v deploy", returnStdout: true).trim().split('\n')
                            echo "${pods}"

                            pods.find {
                                def count = sh(script: "oc describe pod ${it} --namespace ${namespace} | grep -c 'git-commit=${env.COMMIT_ID}'", returnStdout: true).trim()
                                echo "${count} -> ${env.GIT_COMMIT}"
                                count == '1'
                            } != null
                        }

                        while (!waitForPodReadinessImpl()) {
                            echo 'Pod is not available or not ready! Retry after few seconds...'
                            sleep(time: 30, unit: "SECONDS")
                        }

                        echo 'Pod is ready and updated'
                    }
                }
            }

            stage('SATU (master)') {
                when {
                    branch 'master'
                }
                steps {
                    script {

                        def namespace = "${env.BASE_NAMESPACE}-satu"

                        def waitForPodReadinessImpl {
                            def pods = sh(script: "oc get pods --namespace ${namespace} | grep -E \"${env.APP}.*\" | grep -v build | grep -v deploy", returnStdout: true).trim().split('\n')
                            echo "${pods}"

                            pods.find {
                                def count = sh(script: "oc describe pod ${it} --namespace ${namespace} | grep -c 'git-commit=${env.COMMIT_ID}'", returnStdout: true).trim()
                                echo "${count} -> ${env.GIT_COMMIT}"
                                count == '1'
                            } != null
                        }

                        while (!waitForPodReadinessImpl()) {
                            echo 'Pod is not available or not ready! Retry after few seconds...'
                            sleep(time: 30, unit: "SECONDS")
                        }

                        echo 'Pod is ready and updated'
                    }
                }
            }
        }
    }
