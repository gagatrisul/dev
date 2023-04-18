package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.Git

class EcomAuCaBuild {

  public EcomAuCaBuild(
    DslFactory factory,
    String bitbucketCredentialsId,
    String artifactoryApiKeyId,
    String jobNameSuffix,
    String defaultBranch,
    String pipelineScript
  ) {

    factory.pipelineJob("ecom/ecom-${jobNameSuffix.toLowerCase()}-build") {
      logRotator(-1, 50)
      properties {
        disableConcurrentBuilds()
      }
      environmentVariables {
        keepBuildVariables(true)
        keepSystemVariables(true)
        envs([
          BITBUCKET_CREDENTIALS_ID: bitbucketCredentialsId,
          ARTIFACTORY_KEY: artifactoryApiKeyId
        ])
      }
      parameters {
        extensibleChoiceParameterDefinition {
          name('BRANCH')
          choiceListProvider {
            systemGroovyChoiceListProvider {
              groovyScript {
                script(Git.getListRepositoryBranchesScript(
                  'e-commerce',
                  bitbucketCredentialsId
                ))
                sandbox(true)
              }
              usePredefinedVariables(false)
              defaultChoice(defaultBranch)
            }
            editable(true)
            description('')
          }
        }
        choiceParam('DEPLOY_ENV', ['NONE', 'dev3', 'tst4', 'tst6'])
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
