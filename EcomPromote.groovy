package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.Git

class EcomPromote {

  public EcomPromote(
    DslFactory factory,
    String source,
    String target,
    String region,
    String branch,
    String schedule,
    String bitbucketCredentialsId,
    String pipelineScript
  ) {

    factory.pipelineJob("ecom/ecom-${region}-${target}-${source}-promote") {
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
        SOURCE: (source),
        TARGET: (target)
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