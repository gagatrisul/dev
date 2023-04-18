package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.Git

class EcomCleanup {

  public EcomCleanup(
    DslFactory factory,
    String bitbucketCredentialsId,
    String gcrCredentialsId,
    List<String> ecomEmailList,
    String pipelineScript,
    String jobName = 'ecom/ecom-cleanup'
  ) {

    def emailCsv = ecomEmailList.join(', ')

    factory.pipelineJob(jobName) {
      logRotator(-1, 50)
      properties {
        disableConcurrentBuilds()
      }
      environmentVariables {
        keepBuildVariables(true)
        keepSystemVariables(true)
        envs([
          BITBUCKET_CREDENTIALS_ID: bitbucketCredentialsId,
          GCR_CREDENTIALS_ID: gcrCredentialsId,
          EMAIL_RECIPIENTS: emailCsv
        ])
      }
      parameters {
        booleanParam('GIT_TAG_CLEANUP', true, 'Remove older Git tags for ecom builds')
        booleanParam('DOCKER_IMAGE_CLEANUP', true, 'Remove older docker images for ecom builds')
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
