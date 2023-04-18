package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.GCR
import utilities.Credentials

class EcomScale extends EcomDeployBase {

  public EcomScale(
    DslFactory factory,
    Map profile
  ) {
    super(profile.environment, profile.region)
    def scaleScript = "#!/usr/bin/env bash\nset -e\n"
    profile.pods.each { p -> scaleScript = scaleScript + """./devops ${profile.environment}-${profile.region} ./scale.sh ${p[0]} \$${p[0].replaceAll('-','_')}\n""" }
    def scaleIngressScript = """./devops ${profile.environment}-${profile.region} ./kubectl scale --replicas=\$nginx_ingress deployment/nginx-ingress --namespace default \n"""

    def job = factory.job("ecom/ecom-${profile.region}-${profile.environment}-scale-${profile.postfix}") {
      logRotator(-1, 60)
      label(getBuildBox())
      parameters {
        booleanParam('scale_databse', false, "*Warining: This option requires downtime. Scaling to: ${profile.db_cpu} vCPU x ${profile.db_memory} GB")
        profile.pods.each { p ->
          stringParam("${p[0].replaceAll('-','_')}", "${p[1]}", "Number of pods on ${p[0]} deployment.")
        }
        stringParam("nginx_ingress", "${profile.ingress}", "Number of pods on nginx-ingress deployment.")
      }
      scm {
        git {
          remote {
            name('origin')
            url('https://bitbucket.org/rodanandfields/hybris-containers.git')
            credentials(Credentials.getBitbucketCredentialsId())
          }
          branch(getEffectiveBranch())
        }
      }
      wrappers {
        preBuildCleanup()
        buildNameSetter {
          template("#\${BUILD_ID}-ecom-${profile.region}-${profile.environment}-scale-${profile.postfix}")
          runAtStart(true)
          runAtEnd(true)
        }
      }

      steps {
        conditionalSteps{
          condition {
            booleanCondition ( "\$scale_databse" )
          }
          runner ( 'Run' )
          steps {
            shell( """#!/usr/bin/env bash
              set -e
              echo "here the scale down"
              ./devops ${profile.environment}-${profile.region} ./scale.sh hybris-backoffice 0
              ./devops ${profile.environment}-${profile.region} ./scale.sh hybris-backoffice-ca 0
              ./devops ${profile.environment}-${profile.region} ./scale.sh hybris-processengine 0
              ./devops ${profile.environment}-${profile.region} ./scale.sh hybris-storefront 0
              sleep 30 """
            )
            shell( """#!/usr/bin/env bash
              ./devops ${profile.environment}-${profile.region} gcloud sql instances patch ${profile.db_instance_id} --cpu=${profile.db_cpu} --memory=${profile.db_memory}""" 
            )
          }
        }

        shell( scaleScript + scaleIngressScript )
      }
    }
    configureVault(profile.environment, job)
  }

  void configureVault(def environment, def job) {
        if (environment in ["prod"]) {
            job.with {
                parameters {
                    nonStoredPasswordParam('VAULT_TOKEN', 'Deployment Token')
                }
            }
        } else {
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

}
