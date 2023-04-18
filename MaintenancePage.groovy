package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory

class MaintenancePage {

  public MaintenancePage(DslFactory factory, String bitbucketCredentialsId) {

    factory.job("ecom/maintenance-page") {
      logRotator(-1, 50)
      parameters {
        choiceParam('MaintenancePageStatus', ['Up', 'Down'], 'Specifies whether to bring the maintenance page up or down.')
        choiceParam('Scope', ['Internal', 'External', 'Both'], 'Sets the internal or external (or both) maintenance page(s) (not applicable for Legacy environments).')
        choiceParam('PageContent', ['Planned', 'Unplanned'], 'Choose to display planned or unplanned maintenance page verbage (not applicable for R1A).')
        choiceParam('Environment', ['DEV2', 'DEV3', 'TST2', 'TST4', 'TST5', 'TST6', 'TST7', 'TST8', 'PPD2', 'PPD3', 'PROD', 'DR'], 'Environment to bring the maintenance page up or down in.')
        choiceParam('Regions', ['US', 'CA', 'AU', 'JP', 'PULSE', 'R1A', 'R2'], 'The region to be affected within the selected envrionment.')
      }
      label("docker-t2micro")
      scm {
        git {
          remote {
            name('origin')
            url('https://bitbucket.org/rodanandfields/devops-scripts.git')
            credentials(bitbucketCredentialsId)
          }
          branch('master')
        }
      }
      wrappers {
        preBuildCleanup()
        credentialsBinding {
          string {
            variable('MAINTENANCE_PAGE_PASSWORD')
            credentialsId 'e919fc3a-428e-4262-b308-a85ae77eb5e4'
          }
        }
      }
      steps {
        shell(

        )
        shell('''#!/bin/bash

cd maint_page
echo $MAINTENANCE_PAGE_PASSWORD>pwd.txt
ansible-vault decrypt --vault-password-file=pwd.txt maintConfig.ini
''')
        shell('''#!/bin/bash

cd maint_page

python -u maintPage.py $MaintenancePageStatus $Scope $PageContent $Environment $Regions
''')
      }
    }
  }
}