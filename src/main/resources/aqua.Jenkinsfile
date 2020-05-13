    stage('Aqua Reports') {

        environment {
            AQUA_PROJECT_ID = @AQUA_PROJECT_ID@
            AQUA_PRODUCT_ID = @AQUA_PRODUCT_ID@
            AQUA_RELEASE = @AQUA_RELEASE@
            AQUA_LEVEL = @AQUA_LEVEL@
            AQUA_JUNIT_TEST_FOLDER_ID = @AQUA_JUNIT_TEST_FOLDER_ID@
            AQUA_INTEGRATION_TEST_FOLDER_ID = @AQUA_INTEGRATION_TEST_FOLDER_ID@
        }

        steps {

            script {

                def sendToAqua = { file, folderId, testType ->

                    def response = sh(script: """
                    curl -X POST \
                    -H "X-aprojectid: ${env.AQUA_PROJECT_ID}" \
                    -H "X-afolderid: ${folderId}" \
                    -H "X-aprodukt: ${env.AQUA_PRODUCT_ID}" \
                    -H "X-aausbringung: ${env.AQUA_RELEASE}" \
                    -H "X-astufe: ${env.AQUA_LEVEL}" \
                    -H "X-ateststufe: ${testType}" \
                    -H "X-commit: ${env.GIT_COMMIT}" \
                    --data-binary @${file.path} \
                    "http://ju2aqua.ju2aqua-itu.svc.cluster.local/stream/"
                    """, returnStdout: true)

                    if (response != 'OK') {
                        error "Unable to report ${file.path} test in aqua ${folderId} folder!"
                    }
                }

                def reports = findFiles(glob: "**/*Test.xml")
                reports.each { sendToAqua(it, env.JUNIT_TEST_AQUA_FOLDER_ID, 'Komponententest') }

                reports = findFiles(glob: "**/*IT.xml")
                reports.each { sendToAqua(it, env.INTEGRATION_TEST_AQUA_FOLDER_ID, 'Integrationstest') }
            }
        }
    }
