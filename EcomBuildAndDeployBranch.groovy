package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.Git

class EcomBuildAndDeployBranch {

  public EcomBuildAndDeployBranch(
    DslFactory factory,
    String environment,
    String region,
    String bitbucketCredentialsId,
    String pipelineScript
  ) {

    factory.pipelineJob("ecom/ecom-${region.toLowerCase()}-${environment.toLowerCase()}-build-and-deploy-branch") {
      logRotator(-1, 50)
      properties {
        disableConcurrentBuilds()
      }
      environmentVariables {
        keepBuildVariables(true)
        keepSystemVariables(true)
        envs([
          BITBUCKET_CREDENTIALS_ID: bitbucketCredentialsId,
          REGION: region,
          ENVIRONMENT: environment
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
              defaultChoice('develop')
            }
            editable(true)
            description('')
          }
        }
        booleanParam('FORCE_BUILD', false, 'Do not check for changes, build anyway.')
        booleanParam('FORCE_DEPLOY', false, 'Deploy build even if it is already deployed.')
        booleanParam('RUN_UPDATE', false, 'Run system update before deploy. This will also force deployment.')
        booleanParam('SKIP_SONAR_SCAN', false, 'Skip Sonar scan after build.')
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
