package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.Git

class EcomDeployBranchTrigger {

  public EcomDeployBranchTrigger(
    DslFactory factory,
    String environment,
    String region,
    String branch,
    String schedule
  ) {

    factory.job("ecom/ecom-${region.toLowerCase()}-${environment.toLowerCase()}-deploy-branch-trigger") {
      logRotator(-1, 60)
      triggers {
        cron(schedule)
      }
      steps {
        downstreamParameterized {
          trigger("ecom/ecom-${region.toLowerCase()}-${environment.toLowerCase()}-deploy-branch") {
            parameters {
              predefinedProp('BRANCH', branch)
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
