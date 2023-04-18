import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import java.lang.Iterable
import java.util.Iterator

env.CURRENT_COMMIT_ID = ''
env.PREVIOUS_COMMIT_ID = ''
env.BUILD_JOB_NAME = "ecom/ecom-${env.REGION.toLowerCase()}-build"

class Builds implements Iterable<RunWrapper> {

  Builds(String jobName) {
    this.jobName = jobName
  }

  private String jobName

  @NonCPS
  Iterator<RunWrapper> iterator() {
    return new BuildsIterator(this.jobName);
  }
}

class BuildsIterator implements Iterator<RunWrapper> {

  BuildsIterator(String jobName) {
    def workflowRun = Jenkins.instance.getItemByFullName(jobName).getLastBuild()
    this.nextWrapper = workflowRun == null ? null : new RunWrapper(workflowRun, false)
  }

  private RunWrapper nextWrapper;

  @NonCPS
  public boolean hasNext() {
    return this.nextWrapper != null
  }

  @NonCPS
  public RunWrapper next() {
    def temp = this.nextWrapper;
    this.nextWrapper = this.nextWrapper.getPreviousBuild()
    return temp
  }

  // Used to remove an element. Implement only if needed
  public void remove() {
    // Do nothing
  }
}

def getPreviousSuccessfulBuildCommitId(buildJobName) {
  def builds = new Builds(buildJobName)
  def build = builds.find{ it -> it.result == 'SUCCESS' && it.rawBuild.environment.BRANCH == BRANCH }
  def result = build?.buildVariables?.CURRENT_COMMIT_ID?.toString() ?: ''
  return result
}

// Main pipeline
pipeline {

  agent {
    node {
      label 'ecom-builder'
    }
  }

  // all pipeline stages
  stages {

    stage('get-most-recent-commit-id') {
      steps {
        script {
          def gitVariables = git branch: "${BRANCH}", credentialsId: BITBUCKET_CREDENTIALS_ID, url: 'https://bitbucket.org/rodanandfields/e-commerce.git'
          env.CURRENT_COMMIT_ID = gitVariables.GIT_COMMIT
        }
        println("CURRENT_COMMIT_ID: ${env.CURRENT_COMMIT_ID}")
      }
    }

    stage('get-previous-commit-id') {
      steps {
        println("BUILD_JOB_NAME: ${BUILD_JOB_NAME}")
        script {
          env.PREVIOUS_COMMIT_ID = getPreviousSuccessfulBuildCommitId(BUILD_JOB_NAME)
        }
        println("PREVIOUS_COMMIT_ID: ${env.PREVIOUS_COMMIT_ID}")
      }
    }

    stage('Build') {
      when {
        expression { env.CURRENT_COMMIT_ID != env.PREVIOUS_COMMIT_ID }
      }
      steps {
        build(
          job: env.BUILD_JOB_NAME,
          wait: false,
          parameters: [
            [$class: 'StringParameterValue', name: 'BRANCH', value: env.BRANCH],
            [$class: 'StringParameterValue', name: 'DEPLOY_ENV', value: env.DEPLOY_ENV]
          ])
      }
    }

    stage('Set-Build-Name') {
      steps {
        buildName("${BRANCH}${env.PREVIOUS_COMMIT_ID == env.CURRENT_COMMIT_ID ? ' (NO CHANGES DETECTED)' : ''}")
      }
    }

  }
}
