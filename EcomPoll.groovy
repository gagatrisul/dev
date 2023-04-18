package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.Git

class EcomPoll {

  public EcomPoll(
    DslFactory factory,
    String environment,
    String region,
    String branch,
    String schedule,
    String bitbucketCredentialsId,
    String pipelineScript
  ) {

    factory.pipelineJob("ecom/ecom-${region}-${environment}-poll") {
      blockOn("ecom/ecom-${region}-build")
      logRotator(-1, 50)
      properties {
        disableConcurrentBuilds()
        pipelineTriggers {
          triggers {
            cron {
              spec(schedule)
            }
          }
        }
      }
    environmentVariables {
      keepBuildVariables(true)
      keepSystemVariables(true)
      envs([
        BITBUCKET_CREDENTIALS_ID: bitbucketCredentialsId,
        BRANCH: (branch),
        REGION: (region),
        DEPLOY_ENV: (environment)
        ])
      }
    definition {
      cps {
        sandbox(true)
        script(pipelineScript)
        }
      }
    }
  }
}