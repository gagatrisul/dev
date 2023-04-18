package jobs.ecom
import javaposse.jobdsl.dsl.DslFactory
import utilities.GCR

class EcomCertNotify {

  public EcomCertNotify(
    DslFactory factory,
    String environment,
    String bitbucketCredentialsId,
    List<String> ecomEmailList,
    String schedule = '0 11 * * 1'
  ) {

    def emailCsv = ecomEmailList.join(', ')
    def job = factory.job("ecom/ecom-$environment-prod-wild-cert-notify") {
      logRotator(-1, 60)
      triggers {
        cron(schedule)
      }
      label("docker")
      scm {
        git {
          remote {
            name('origin')
            url('https://bitbucket.org/rodanandfields/devops-scripts.git')
            credentials(bitbucketCredentialsId)
          }
          branch("master")
        }
      }
      steps {
        shell("cd wild-cert-notify && ./devops $environment ./notify.sh")
      }
      publishers {
        downstreamParameterized {
          trigger('utilities/email-deployment-status') {
            condition('FAILED')
            parameters {
              if ( environment == 'us') {
                predefinedProp('SUBJECT', "Please update the certificates for ${environment.toUpperCase()}: wild-rodanandfields-com")
              }
              else if ( environment == 'jp') {
                predefinedProp('SUBJECT', "Please update the certificates for ${environment.toUpperCase()}: wild-rodanandfields-co-jp")
              }
              predefinedProp('EMAIL_RECIPIENTS', emailCsv)
            }
          }
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
            addrVariable("VAULT_ADDR")
            tokenVariable("VAULT_TOKEN")
            vaultAddr("https://vault.rodanandfields.com")
            credentialsId("non-prod-ro-vault-rodanandfields-com")
          }
        }
      }
    }
  }
}
