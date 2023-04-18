package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory

class EcomAuCaDeploy {

  public EcomAuCaDeploy(
    DslFactory factory,
    String environment,
    String region,
    String bitbucketCredentialsId,
    List<String> ecomEmailList
  ) {

    def emailCsv = ecomEmailList.join(', ')

    factory.job("ecom/ecom-${region}-${environment}-deploy") {
      logRotator(-1, 60)
      parameters {
        extensibleChoiceParameterDefinition {
          name('BUILD_VERSION')
          choiceListProvider {
            systemGroovyChoiceListProvider {
              groovyScript {
                script("""import groovy.json.JsonSlurper
def rgn = '${region}'
def envm = '${environment}'
def uri = ''
def buildNumbers = []
def bld_text = new URL( "http://artifactory.rodanandfields.com:8081/artifactory/api/storage/rf-snapshot-local/ecom").text
def jsonSlurper = new JsonSlurper()
def result = jsonSlurper.parseText(bld_text)
result.children.each {
  regexp = /.*/

  if ( rgn != "" ) {
    rgn = rgn.substring(0,2).toUpperCase()
  }

  if ( envm == "prod" || envm == "ppd3" ) {
    regexp = /ecom-\${rgn}-ECOM-(REL|HOTFIX).*/
  }

  uri = it.uri.trim().substring(1)

  if ( uri =~ regexp ) {
    buildNumbers.add(uri)
  }

  /*buildNumbers.add(it.uri.trim().substring(1))*/
}
return buildNumbers""")
                sandbox(false)
              }
              usePredefinedVariables(false)
              defaultChoice(null)
            }
            editable(true)
            description('')
          }
        }
      }
      label('ecom-builder')
      scm {
        git {
          remote {
            name('origin')
            url('https://bitbucket.org/rodanandfields/ansible-infrastructure.git')
            credentials(bitbucketCredentialsId)
          }
          branch('rhel-windows-config')
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
              predefinedProp('EMAIL_RECIPIENTS', emailCsv)
            }
          }
        }
        envInjectBuilder {
            propertiesFilePath('')
            propertiesContent('HYBRIS_BUILD_URL=http://artifactory.rodanandfields.com:8081/artifactory/rf-snapshot-local/ecom/${BUILD_VERSION}/')
        }
        shell("""#!/usr/bin/env bash
ansible-playbook -i inventory/${environment}/${region}/hosts.ini -e "DeploymentPackageURL=\${HYBRIS_BUILD_URL}" roles/hybris_${region}/tasks/${environment}.yml -vvv\n\
""")
      }
      publishers {
        downstreamParameterized {
          if (environment == 'tst4' && region in ['au', 'ca']) {
            trigger("ecom/ecom-${region}-tst4-jira-build-to-qa") {
              condition('FAILED_OR_BETTER')
              triggerWithNoParameters(true)
            }
          }
          trigger("utilities/email-deployment-status") {
            condition('ALWAYS')
            parameters {
              predefinedProp('SUBJECT', "\${BUILD_VERSION} Deployment to ${environment.toUpperCase()} ${region.toUpperCase()}")
              predefinedProp('BUILD_PROJECT_NAME', "ecom-build")
              predefinedProp('BUILD_BUILD_VERSION', '${BUILD_VERSION}')
              predefinedProp('DEPLOY_PROJECT_NAME', '${JOB_NAME}')
              predefinedProp('DEPLOY_BUILD_NUMBER', '${BUILD_NUMBER}')
              predefinedProp('EMAIL_RECIPIENTS', emailCsv)
            }
          }
        }
      }
    }
  }
}
