package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory

class EcomDockerCleanupTrigger {

  public EcomDockerCleanupTrigger(DslFactory factory, String nodeLabel = 'docker', String cronSchedule = 'H 1 * * *') {

    factory.job("ecom/ecom-docker-cleanup-trigger-${nodeLabel}-label") {
      logRotator(-1, 10)
      triggers {
        cron(cronSchedule)
      }
      steps {
        downstreamParameterized {
          trigger('utilities/docker-cleanup') {
            parameters {
              predefinedProp('LABEL', nodeLabel)
            }
          }
        }
      }
    }
  }
}
