package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.Git

class EcomBuildAndDeployBranchTrigger {

  public EcomBuildAndDeployBranchTrigger(
    DslFactory factory,
    String environment,
    String region,
    String branch,
    String schedule,
    Boolean skipSonar = false,
    Boolean enabled = true
  ) {

    factory.job("ecom/ecom-${region.toLowerCase()}-${environment.toLowerCase()}-build-and-deploy-branch-trigger") {
      if (!enabled) {
        disabled()
      }
      logRotator(-1, 60)
      triggers {
        cron(schedule)
      }
      steps {
        downstreamParameterized {
          trigger("ecom/ecom-${region.toLowerCase()}-${environment.toLowerCase()}-build-and-deploy-branch") {
            parameters {
              predefinedProp('BRANCH', branch)
              booleanParam('SKIP_SONAR_SCAN', skipSonar)
            }
            block {
              buildStepFailure("FAILURE")
              failure("FAILURE")
              unstable("UNSTABLE")
            }
          }
        }
      }
    }
  }
}
