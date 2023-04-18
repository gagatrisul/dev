package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.GCR

class EcomDeploy extends EcomDeployBase {

  public EcomDeploy(
    DslFactory factory,
    String environment,
    String region,
    String bitbucketCredentialsId,
    String newRelicApiKey,
    List<String> newRelicApplicationIds,
    List<String> ecomEmailList
  ) {
    super(environment, region)

    def job = factory.job("ecom/ecom-${region}-${environment}-deploy") {
      logRotator(-1, 60)
      parameters {
        extensibleChoiceParameterDefinition {
          name('BUILD_VERSION')
          choiceListProvider {
            systemGroovyChoiceListProvider {
              groovyScript {
                script(GCR.getListImageTagsScript('hybris', environment, region))
                sandbox(false)
              }
              usePredefinedVariables(false)
              defaultChoice(null)
            }
            editable(true)
            description('')
          }
        }
        booleanParam('RUN_UPDATE', false, 'FORCE full system update and non-rolling restart')
        stringParam('FAKETIME', '', 'Time adjustment in the deployed hybris containers in hours, e.g. 48 or -72')
        textParam('LOCAL_PROPERTIES', '', 'Additional custom values to local.properties. It is not persisted and valid for this deployment only.')
        booleanParam('DRY_RUN', false, 'Experimental! Prints the output, no commands run in cluster')
        booleanParam('SKIP_UPDATE', false, 'Experimental: DO NOT USE! Has priority over run_update')
        booleanParam('NO_ROLLING', false, 'Experimental: DO NOT USE! Always do delete & install instead of upgrade')
        booleanParam('NO_MAINTENANCE', false, 'Experimental: DO NOT USE! Do not place maintenance page')
      }
      label(getBuildBox())
      scm {
        git {
          remote {
            name('origin')
            url('https://bitbucket.org/rodanandfields/hybris-containers.git')
            credentials(bitbucketCredentialsId)
          }
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
      configure { project ->
       project / publishers << 'jenkins.plugins.slack.SlackNotifier' {
       teamDomain("rodanandfields")
       room("#ci-${environment.toLowerCase()}")
       notifyAborted(false)
       notifyFailure(true)
       notifyNotBuilt(false)
       notifyUnstable(false)
       notifyBackToNormal(false)
       notifySuccess(true)
       notifyRepeatedFailure(true)
       startNotification(true)
       includeCustomMessage(false)
       includeTestSummary(false)
       commitInfoChoice("NONE")
       /*customMessage("@here Attention")
       authToken("blablablabla")*/
       }
      }
      steps {
        downstreamParameterized {
          trigger('utilities/email-commencing-deployment') {
            parameters {
              predefinedProp('DEPLOYMENT_NAME', '${JOB_NAME}')
              predefinedProp('DEPLOYMENT_LINK', '${BUILD_URL}')
              predefinedProp('EMAIL_RECIPIENTS', ecomEmailList.join(', '))
            }
          }
        }
        shell("""#!/usr/bin/env bash
./devops ${envRegion} ./deploy-hybris.sh \${BUILD_VERSION} \${more_deploy_args}
""")
        /*environmentVariables {
            propertiesFile('env_upd.properties')
        }*/
      }
      publishers {
          if (environment in ['dev3', 'dev3up', 'tst4', 'tst9', 'ppd3', 'tst6'] && region in ['australia', 'us']) {
          groovyPostBuild("""import jenkins.model.* 
                          import hudson.model.*
                          if(manager.logContains(".*Invalid JWT Signature.*")) {
                          def job =  Jenkins.getInstance().getItemByFullName("ecom/ecom-${region}-${environment}-deploy") 
                          String bv=manager.build.buildVariables.get("BUILD_VERSION")
                          def params = (new  StringParameterValue('BUILD_VERSION', bv))
                          def future = job.scheduleBuild2(0, new ParametersAction(params))}""",  Behavior.MarkFailed)
        }
        if (newRelicApplicationIds != null && newRelicApplicationIds.size() > 0) {
          newRelicDeploymentNotifier {
            notifications {
              for (appId in newRelicApplicationIds) {
                deploymentNotificationBean {
                  apiKey(newRelicApiKey)
                  applicationId(appId)
                  description('')
                  revision('${BUILD_VERSION}')
                  changelog('')
                  user('')
                }
              }
            }
          }
        }
        downstreamParameterized {
          if (environment.startsWith('dev') && region in ['us', 'ca']) {
            trigger("network-infra/akamai-cache-clear") {
              condition('FAILED_OR_BETTER')
              parameters {
                predefinedProp('NETWORK', 'production')
                predefinedProp('TARGET_ENV', 'DEV')
              }
            }
          } 
          if (environment.startsWith('tst') && region in ['us', 'ca']) {
            trigger("network-infra/akamai-cache-clear") {
              condition('FAILED_OR_BETTER')
              parameters {
                predefinedProp('NETWORK', 'production')
                predefinedProp('TARGET_ENV', 'TST')
              }
            }
          }
          if (environment in ['dev3', 'tst4', 'tst9', 'ppd3', 'tst6'] && region in ['us', 'jp', 'ca']) {
            trigger("ecom/QA_JG_ecom-${region}-${environment}-smoke-test") {
              condition('FAILED_OR_BETTER')
              parameters {
                predefinedProp('BUILD_VERSION', '${BUILD_VERSION}')
                predefinedProp('DEPLOY_PROJECT_NAME', '${JOB_NAME}')
                predefinedProp('DEPLOY_BUILD_NUMBER', '${BUILD_NUMBER}')
              }
            }
          }
          if (environment in ['dev3', 'tst4', 'tst9', 'ppd3', 'tst6'] && region in ['australia']) {
            trigger("ecom/QA_JG_ecom-au-${environment}-smoke-test") {
              condition('FAILED_OR_BETTER')
              parameters {
                predefinedProp('BUILD_VERSION', '${BUILD_VERSION}')
                predefinedProp('DEPLOY_PROJECT_NAME', '${JOB_NAME}')
                predefinedProp('DEPLOY_BUILD_NUMBER', '${BUILD_NUMBER}')
              }
            }
          }
          // else {
          //   trigger("ecom/ecom-${region}-${environment}-smoke-test") {
          //     condition('FAILED_OR_BETTER')
          //     parameters {
          //       predefinedProp('BUILD_VERSION', '${BUILD_VERSION}')
          //       predefinedProp('DEPLOY_PROJECT_NAME', '${JOB_NAME}')
          //       predefinedProp('DEPLOY_BUILD_NUMBER', '${BUILD_NUMBER}')
          //     }
          //   }
          // }
          // if (environment == 'tst4' && region in ['us', 'jp']) {
          //   trigger("ecom/ecom-${region}-tst4-jira-build-to-qa") {
          //     condition('FAILED_OR_BETTER')
          //     triggerWithNoParameters(true)
          //   }
          // }
          // if (environment == 'tst4' && region == 'us') {
          //   trigger("qa/qa-automation-by-type/bat/bat-us/bat-us-${environment}-sf") {
          //     condition('FAILED_OR_BETTER')
          //     triggerWithNoParameters(true)
          //   }
          //   trigger("qa/qa-automation-by-type/bat/bat-ca/bat-ca-${environment}-sf") {
          //     condition('FAILED_OR_BETTER')
          //     triggerWithNoParameters(true)
          //   }
          // }
          // if (environment == 'tst4' && region in ['australia', 'jp']) {
          //   trigger("qa/qa-automation-by-type/bat/bat-${region}/bat-${region}-${environment}-sf") {
          //     condition('FAILED_OR_BETTER')
          //     triggerWithNoParameters(true)
          //   }
          // }
          trigger("utilities/email-deployment-status") {
            condition('FAILED')
            parameters {
              predefinedProp('SUBJECT', 'Failed ${JOB_NAME} ${BUILD_VERSION}')
              predefinedProp('BUILD_PROJECT_NAME', "ecom-build")
              predefinedProp('BUILD_BUILD_VERSION', '${BUILD_VERSION}')
              predefinedProp('DEPLOY_PROJECT_NAME', '${JOB_NAME}')
              predefinedProp('DEPLOY_BUILD_NUMBER', '${BUILD_NUMBER}')
              predefinedProp('EMAIL_RECIPIENTS', ecomEmailList.join(', '))
            }
          }
        }
      }
    }
    configureVault(job)
  }

    boolean isRollingDefault() {
        return environment.startsWith('ppd') || environment.startsWith('prod')
    }
}
