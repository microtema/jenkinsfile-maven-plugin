def triggerJob(jobName) {

    build job: jobName,
          wait: true,
          parameters: [
              string( name: 'git_commit', value: env.COMMIT_ID),
              string( name: 'stage_name', value: env.STAGE_NAME),
              string( name: 'maven_version', value: env.VERSION)
          ]
}
