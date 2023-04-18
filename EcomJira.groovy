package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory

class EcomJira {

    public EcomJira(
      DslFactory factory,
      String environment,
      String region,
      String query
	) {

    factory.job("ecom/ecom-${region}-${environment}-jira-build-to-qa") {
      logRotator(-1, 50)
	  
	  steps {
        progressJiraIssues {
            jqlSearch"${query}"
            workflowActionName('QA')
            comment('Automatic Update by Jenkins')
        }
      }
	  publishers {
        extendedEmail {
            recipientList('nosend-devopsBandE@rodanandfields.com')
            defaultSubject('Updating tickets to QA')
            defaultContent('${BUILD_LOG}')
		}
	  }
    }
  }
}