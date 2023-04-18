package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.Git

class EcomMediaBackup2 extends EcomDeployBase {

  public EcomMediaBackup2(
    DslFactory factory,
    String environment,
    String region,
    String bitbucketCredentialsId,
    String emailCsv,
    String interval = 'daily',
    String schedule = '0 2 * * *',
    Boolean dbBackup = false
  ) {
    super(environment, region)

    def job = factory.job("ecom/ecom2-${region}-${environment}-${interval}-backup") {
      logRotator(-1, 60)
      label(getBuildBox())
    //   triggers {
    //     cron(schedule)
    //   }
      properties {
        disableConcurrentBuilds()
      }
      environmentVariables {
        keepBuildVariables(true)
        keepSystemVariables(true)
        envs([
          BITBUCKET_CREDENTIALS_ID: bitbucketCredentialsId
        ])
      }
      parameters {
        booleanParam('DB_BACKUP_TOO', dbBackup, 'Toggle this if DB backup is needed as well')
      }
      wrappers {
        buildNameSetter {
          template("#\${BUILD_ID}-${envRegion}")
          runAtStart(true)
          runAtEnd(true)
        }
      }
      scm {
        git {
          remote {
            name('origin')
            url('https://bitbucket.org/rodanandfields/hybris-containers.git')
            credentials(bitbucketCredentialsId)
          }
          branch("prod-${region}-DO-8631")
          // branch(getEffectiveBranch())
        }
      }
      steps {
        shell("""#!/usr/bin/env bash\n\
[[ "\${DB_BACKUP_TOO}" == "true" ]] && include_db="--include-db"
./devops ${envRegion} ./media-backup.sh  ${interval} \${include_db}
""")
      }
      publishers {
        extendedEmail {
          recipientList(emailCsv)
          defaultSubject("Backup Started: #\${BUILD_ID}-${envRegion} with DB: \${DB_BACKUP_TOO}")
          defaultContent('You should receive the completion email when backup is finished...')
          contentType('text/plain')
          triggers {
            beforeBuild {
              sendTo{
                recipientList()
              }
            }
            success {
              subject("Backup Completed: #\${BUILD_ID}-${envRegion} with DB: \${DB_BACKUP_TOO}")
              attachBuildLog()
              content("Data and log folder on the target Filestore: \"${envRegion}-backup\"\nFor more backup parameters see \"hybris-containers/envs/${environment}/${region}/values.yml\"")
              sendTo{
                recipientList()
              }
            }
          }
        }
      }
      configure { project ->
       project / publishers << 'jenkins.plugins.slack.SlackNotifier' {
       teamDomain("rodanandfields")
       room("#ci-cd")
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
    }
    configureVault(job)
  }

    void configureVault(def job) {
        job.with {
            wrappers {
                credentialsBinding {
                    vaultTokenCredentialBinding {
                        addrVariable('VAULT_ADDR')
                        tokenVariable('VAULT_TOKEN')
                        vaultAddr('https://vault.rodanandfields.com')
                        credentialsId(prod ? 'prod-deployer-vault-rodanandfields-com' : 'non-prod-ro-vault-rodanandfields-com')
                    }
                }
            }
        }
    }

}
