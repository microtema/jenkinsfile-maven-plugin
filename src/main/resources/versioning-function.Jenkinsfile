def nextVersion(pomVersion) {
    if(!pomVersion.contains('SNAPSHOT')) {
        return pomVersion
    }
    def version = fileExists("version") ? readFile("version") : (pomVersion.split('\\.')[0] + "." + pomVersion.split('\\.')[1])
    withGit {
        sh 'git fetch --tags'
        def gitTags = sh(script: 'git tag -l', returnStdout: true)
        return cicd.VersionManager.getNextVersion(gitTags, version, this)
    }
}
