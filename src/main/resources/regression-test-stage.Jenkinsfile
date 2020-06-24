stage(@STAGE_DISPLAY_NAME@) {

    environment {
        JOB_NAME = @JOB_NAME@
        STAGE_NAME = @STAGE_NAME@
        VERSION = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout $MAVEN_ARGS', returnStdout: true).trim()
    }

    when {
        branch @BRANCH_PATTERN@
    }

    steps {

        build job: "../${env.JOB_NAME}/${env.BRANCH_NAME}",
              wait: true,
              parameters: [
                  string( name: 'git_commit', value: env.COMMIT_ID),
                  string( name: 'stage_name', value: env.STAGE_NAME),
                  string( name: 'maven_version', value: env.VERSION)
              ]
    }
}
