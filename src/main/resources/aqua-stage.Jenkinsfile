stage(@STAGE_NAME@) {
    steps {
        script {
            def reports = findFiles glob: @AQUA_FILE_FILTER@
            reports.each { sendToAqua it, @AQUA_FOLDER_ID@, @AQUA_TEST_TYPE@ }
        }
    }
}
