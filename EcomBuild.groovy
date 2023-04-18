package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.Git

class EcomBuild {

  public EcomBuild(
    DslFactory factory,
    String bitbucketCredentialsId,
    String awsCredentialsId,
    String gcrCredentialsId,
    List<String> ecomEmailList,
    String pipelineScript,
    String jobName = 'ecom/ecom-build'
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
          AWS_CREDENTIALS_ID: awsCredentialsId,
          GCR_CREDENTIALS_ID: gcrCredentialsId,
          EMAIL_RECIPIENTS: emailCsv
        ])
      }
      parameters {
        extensibleChoiceParameterDefinition {
          name('BRANCH')
          choiceListProvider {
            systemGroovyChoiceListProvider {
              groovyScript {
                script(Git.getListRepositoryBranchesScript(
                  'ecom-atlas',
                  bitbucketCredentialsId
                ))
                sandbox(true)
              }
              usePredefinedVariables(false)
              defaultChoice(null)
            }
            editable(true)
            description('')
          }
        }
        booleanParam('SKIP_SONAR_SCAN', false, 'For debugging purposes only.')
        /*booleanParam('DO_NOT_PUSH_IMAGE', false, 'For debugging purposes.')*/
        booleanParam('FORCE_BUILD', false, 'For debugging purposes only.')
        choiceParam('BUILD_REGIONS', ['ALL', 'US', 'JP', 'AU'], 'For debugging purposes only.')
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
