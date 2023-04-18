package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory

class EcomSmokeTest {

  public EcomSmokeTest(
    DslFactory factory,
    String environment,
    String region,
    List<String> ecomEmailList
  ) {

    def emailCsv = ecomEmailList.join(', ')

    factory.job("ecom/ecom-${region.toLowerCase()}-${environment.toLowerCase()}-smoke-test") {
      logRotator(-1, 30)
      parameters {
        stringParam('BUILD_VERSION', '', '')
        stringParam('DEPLOY_PROJECT_NAME', '', '')
        stringParam('DEPLOY_BUILD_NUMBER', '', '')
      }
      label('docker-t2micro')
      wrappers {
        buildTimeoutWrapper {
          strategy {
            absoluteTimeOutStrategy {
              timeoutMinutes("10")
            }
          }
          timeoutEnvVar("TIMEOUT_ERROR")
        }
        buildNameSetter {
          template('${BUILD_VERSION}')
          runAtStart(true)
          runAtEnd(true)
        }
      }
      steps {
        downstreamParameterized {
          trigger("qa/qa-automation-by-type/bat/bat-${region.toLowerCase()}/bat-${region.toLowerCase()}-${environment.toLowerCase()}-sf") {
            block {
              buildStepFailure("FAILURE")
              failure("FAILURE")
              unstable("UNSTABLE")
            }
          }
        }
      }
      publishers {
        downstreamParameterized {
          trigger('utilities/email-deployment-status') {
            condition('ALWAYS')
            parameters {
              predefinedProp('SUBJECT', "\${BUILD_VERSION} Deployment to ${environment.toUpperCase()}")
              predefinedProp('BUILD_PROJECT_NAME', "ecom-build")
              predefinedProp('BUILD_BUILD_VERSION', '${BUILD_VERSION}')
              predefinedProp('DEPLOY_PROJECT_NAME', '${DEPLOY_PROJECT_NAME}')
              predefinedProp('DEPLOY_BUILD_NUMBER', '${DEPLOY_BUILD_NUMBER}')
              predefinedProp('SMOKE_PROJECT_NAME', '${JOB_NAME}')
              predefinedProp('SMOKE_BUILD_NUMBER', '${BUILD_NUMBER}')
              predefinedProp('EMAIL_RECIPIENTS', emailCsv)
            }
          }
        }
      }
    }
  }
}
