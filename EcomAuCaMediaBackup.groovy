package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory

class EcomAuCaMediaBackup {

  public EcomAuCaMediaBackup(
    DslFactory factory,
    String environment,
    String region,
    String bitbucketCredentialsId
  ) {

    factory.job("ecom/ecom-${region}-${environment}-backup") {
      logRotator(-1, 50)
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
      triggers {
        cron("00 17 * * *")
      }
      wrappers {
        preBuildCleanup()
      }
      steps {
        shell("""#!/usr/bin/env bash
ansible-playbook -i inventory/${environment}/${region}/hosts.ini roles/media_backup/tasks/${environment}-${region}.yml -vvv\n\
""")
      }
    }
  }
}