package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory
import utilities.GCR
import utilities.Credentials

class EcomHacDeploy extends EcomDeployBase {

  public EcomHacDeploy(
    DslFactory factory,
    String environment,
    String region,
    String bitbucketCredentialsId
  ) {
    super(environment, region)
    def job = factory.job("ecom/ecom-${region}-${environment}-expose-hac-deploy") {
      logRotator(-1, 60)
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
      steps {
        shell("""#!/usr/bin/env bash\n\
set -e
./devops ${envRegion} ./expose-hac.sh --direct
""")
      }
    }
      configureVault(job)
  }
}
