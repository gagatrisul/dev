package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.Git

class EcomDeployBranch {

  public EcomDeployBranch(
    DslFactory factory,
    String environment,
    String region,
    String pipelineScript,
    String bitbucketCredentialsId
  ) {
      factory.pipelineJob("ecom/ecom-${region}-${environment}-deploy-branch") {
      logRotator(-1, 60)
      properties {
        disableConcurrentBuilds()
        rebuild {
          // Rebuilds job without asking for parameters.
          autoRebuild(true)
        }
      }
      environmentVariables {
        keepBuildVariables(true)
        keepSystemVariables(true)
        envs([
          ENVIRONMENT: environment.toLowerCase(),
          REGION: region.toLowerCase()
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
            description('Branch to check for new builds against')
          }
        }
        booleanParam('RUN_UPDATE', false, 'Run system update before deploy')
      }
      definition {
        cps {
          sandbox(false)
          script(pipelineScript)
        }
      }
    }
  }
}
