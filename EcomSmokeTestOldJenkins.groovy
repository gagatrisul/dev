package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory

class EcomSmokeTestOldJenkins {

  public EcomSmokeTestOldJenkins(
    DslFactory factory,
    String region,
    String environment,
    List<String> ecomEmailList
  ) {
    def String folder
    def String safari_browser
    def String test_case_browser
    def String suite_name
    def emailCsv = ecomEmailList.join(', ')

    factory.job("ecom/QA_JG_ecom-${region.toLowerCase()}-${environment.toLowerCase()}-smoke-test") {
      logRotator(-1, 30)
      parameters {
        stringParam('BUILD_VERSION', '', '')
        stringParam('DEPLOY_PROJECT_NAME', '', '')
        stringParam('DEPLOY_BUILD_NUMBER', '', '')
      }
      wrappers {
        buildTimeoutWrapper {
          strategy {
            absoluteTimeOutStrategy {
              timeoutMinutes("75")
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
        if (environment in ['dev3', 'tst4', 'ppd3', 'tst6']) {
          if (region in ['us', 'jp']) { folder = 'BAT_' }
          else { folder = 'BAT_Apollo' }
          for (device in ['Desktop','Mobile']) {
              if (device == 'Desktop') {
                    if (region == 'us') { suite_name = 'us_smoke' }
                    if (region == 'jp') { suite_name = 'jp_smoke' }
                    if (region == 'au') { suite_name = 'au_smoke' }
                    if (region == 'ca') { suite_name = 'ca_smoke' }
                    remoteTrigger('https://jenkins-qa.rodanandfields.com', "QA-K8s/BAT/${region.toUpperCase()}/${device}/Chrome/BAT-${environment.toUpperCase()}-${region.toUpperCase()}-SF-deploy-Chrome") {
                    parameter('SUITESTORUN', suite_name)
                    parameter('BROWSER','chrome')
                    parameter('FreeGift_Flag','true')
                    parameter('NewPDP_Flag','true')
                    parameter('BUILD_VERSION','latest')
                    blockBuildUntilComplete()
                    preventRemoteBuildQueue()
                    shouldNotFailBuild()
                  }
                  if (!['dev3', 'tst6'].contains(environment) && region != 'ca') {
                    if (region == 'us') { safari_browser = 'SF-Safari'; suite_name = 'us_smoke' }
                    if (region == 'au') { safari_browser = 'SAFARI'; suite_name = 'au_smoke' }
                    if (region == 'jp') { safari_browser = 'SF-SAFARI'; suite_name = 'jp_smoke' }
                    remoteTrigger('https://jenkins-qa.rodanandfields.com', "QA-K8s/BAT/${region.toUpperCase()}/${device}/Safari/BAT-${environment.toUpperCase()}-${region.toUpperCase()}-SF-deploy-Safari") {
                    parameter('SUITESTORUN', suite_name)  
                    parameter('BUILD_VERSION','latest')
                    blockBuildUntilComplete()
                    preventRemoteBuildQueue()
                    shouldNotFailBuild()
                    }   
                  }
                }
              if (device == 'Mobile' && !['dev3', 'tst6'].contains(environment) && region != 'ca') {
                for (browser in ['Android', 'Iphone']) {
                    if (region == 'us'){ test_case_browser = "SF-${browser}"; suite_name = 'us_smoke' }
                    if (region == 'au'){ test_case_browser = browser.toUpperCase(); suite_name = 'au_smoke'  }
                    if (region == 'jp'){ test_case_browser = "SF-${browser.toUpperCase()}"; suite_name = 'jp_smoke'  }
                    remoteTrigger('https://jenkins-qa.rodanandfields.com', "QA-K8s/BAT/${region.toUpperCase()}/${device}/${browser}/BAT-${environment.toUpperCase()}-${region.toUpperCase()}-SF-deploy-${browser}") {
                    parameter('SUITESTORUN', suite_name)
                    parameter('BUILD_VERSION','latest')
                    blockBuildUntilComplete()
                    preventRemoteBuildQueue()
                    shouldNotFailBuild()
                  }
                }  
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