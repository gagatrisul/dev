package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.GCR

class MaintenancePageDeploy extends EcomDeployBase {

  public MaintenancePageDeploy(
    DslFactory factory,
    String environment,
    String region,
    String bitbucketCredentialsId
  ) {
    super(environment, region)
    
    factory.job("maintenance-page/maintenance-page-${region}-${environment}-deploy") {
      logRotator(-1, 60)
      parameters {
        extensibleChoiceParameterDefinition {
          name('BUILD_VERSION')
          choiceListProvider {
            systemGroovyChoiceListProvider {
              groovyScript {
                script(GCR.getListImageTagsScript("maintenance-us/unplanned"))
                sandbox(false)
              }
              usePredefinedVariables(false)
              defaultChoice(null)
            }
            editable(true)
            description('')
          }
        }
        booleanParam('UPDATE_GKE', true, 'Deploy to GKE cluster')
        booleanParam('UPDATE_CLOUD_RUN', true, 'Deploy to Cloud Run')
      }
      label("build-${region}-gcpops.rodanandfields.com")
      scm {
        git {
          remote {
            name('origin')
            url('https://bitbucket.org/rodanandfields/hybris-containers.git')
            credentials(bitbucketCredentialsId)
          }
          // TODO: This needs to be merged back into main branch
          branch(getEffectiveBranch())
        }
      }
      wrappers {
        preBuildCleanup()
        buildNameSetter {
          template('#${BUILD_ID}-${BUILD_VERSION}')
          runAtStart(true)
          runAtEnd(true)
        }
      }
      steps {
        shell("""#!/usr/bin/env bash
set -e

cd envs/${environment}/${region}

./deploy-maintenance-page.sh \${BUILD_VERSION}
""")
      }
    }
  }
}
