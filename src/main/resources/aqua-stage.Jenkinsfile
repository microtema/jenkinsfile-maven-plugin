stage(@STAGE_NAME@) {
    steps {
        script {
            def sendToAqua = { file, folderId, testType ->

                def response = sh script: """
                curl -X POST \
                -H "X-aprojectid: ${env.AQUA_PROJECT_ID}" \
                -H "X-afolderid: ${folderId}" \
                -H "X-aprodukt: ${env.AQUA_PRODUCT_ID}" \
                -H "X-aausbringung: ${env.AQUA_RELEASE}" \
                -H "X-astufe: ${env.AQUA_LEVEL}" \
                -H "X-ateststufe: ${testType}" \
                -H "X-commit: ${env.GIT_COMMIT}" \
                --data-binary @${file.path} \
                "${env.AQUA_URL}"
                """, returnStdout: true

                if (response != 'OK') {
                    error "Unable to report ${file.path} test in aqua ${folderId} folder!"
                }
            }

            def reports = findFiles glob: @AQUA_FILE_FILTER@
            reports.each { sendToAqua it, @AQUA_FOLDER_ID@, @AQUA_TEST_TYPE@ }
        }
    }
}
