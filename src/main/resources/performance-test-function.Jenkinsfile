def publishJMeterReport() {
    def reports = sh(script: 'ls -d target/jmeter/reports/*', returnStdout: true).trim().split('\n')
    reports.each {
        publishHTML target: [allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true,
                             reportDir   : it, escapeUnderscores: true,
                             reportFiles : 'index.html', reportName: "Performance Tests"]
    }
}
