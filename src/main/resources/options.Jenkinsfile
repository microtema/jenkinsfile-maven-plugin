options {
    disableConcurrentBuilds()
    timeout(time: @TIMEOUT@, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
}
