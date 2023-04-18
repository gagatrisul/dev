import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import java.lang.Iterable
import java.util.Iterator

env.CURRENT_COMMIT_ID = ''
env.PREVIOUS_COMMIT_ID = ''
env.BUILD_IMAGE = ''
env.DEPLOYED_IMAGE = ''
env.IMAGE_VARIABLE = env.REGION.toLowerCase()
env.BUILD_JOB_NAME = "ecom/ecom-build"
env.DEPLOY_JOB_NAME = "ecom/ecom-${env.REGION.toLowerCase()}-${env.ENVIRONMENT.toLowerCase()}-deploy"

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
  def build = builds.find {
    it -> it.result == 'SUCCESS' && it.rawBuild.environment.BRANCH == BRANCH && (it.rawBuild.environment.BUILD_REGIONS == env.REGION.substring(0,2) || it.rawBuild.environment.BUILD_REGIONS == 'ALL')
  }
  def result = build?.buildVariables?.CURRENT_COMMIT_ID?.toString() ?: ''
  return result
}

def getPreviousSuccessfulBuildImage(buildJobName) {
  def builds = new Builds(buildJobName)
  def build = builds.find{ it ->
    return it.result == 'SUCCESS' && it.rawBuild.environment.BRANCH == BRANCH && it?.buildVariables?."${env.IMAGE_VARIABLE}"
  }
  def result = build?.buildVariables?."${env.IMAGE_VARIABLE}"?.toString() ?: ''
  return result
}

def getCurrentDeployedImage(deployJobName) {
  def builds = new Builds(deployJobName)
  def build = builds.find{ it -> it.result == 'SUCCESS' }
  def result = build?.buildVariables?.BUILD_VERSION?.toString() ?: ''
  return result
}

// Main pipeline
pipeline {
  /*
  * Run everything on an existing agent configured with a label 'docker'.
  * This agent will need docker, git and a jdk installed at a minimum.
  */
  agent {
    node {
      label 'docker'
    }
  }

  // all pipeline stages
  stages {

    stage('get-most-recent-commit-id') {
      steps {
        script {
          def gitVariables = git branch: "${BRANCH}", credentialsId: BITBUCKET_CREDENTIALS_ID, url: 'https://bitbucket.org/rodanandfields/ecom-atlas.git'
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

    stage('build') {
      when {
        expression { env.CURRENT_COMMIT_ID != env.PREVIOUS_COMMIT_ID }
      }
      steps {
        build(
          job: env.BUILD_JOB_NAME,
          wait: true,
          parameters: [
            [$class: 'StringParameterValue', name: 'BRANCH', value: env.BRANCH],
            [$class: 'BooleanParameterValue', name: 'FORCE_BUILD', value: env.FORCE_BUILD],
            [$class: 'BooleanParameterValue', name: 'SKIP_SONAR_SCAN', value: env.SKIP_SONAR_SCAN],
            [$class: 'StringParameterValue', name: 'BUILD_REGIONS', value: env.REGION.substring(0,2).toUpperCase()]
          ])
      }
    }

    stage('get-most-recent-image') {
      steps {
        script {
          // adapted for non-binary regions
          if (env.IMAGE_VARIABLE == 'us') {
            env.IMAGE_VARIABLE='US_IMAGE_TAG'
          } else if (env.IMAGE_VARIABLE == 'jp') {
            env.IMAGE_VARIABLE='JP_IMAGE_TAG'
          } else if (env.IMAGE_VARIABLE == 'au' || IMAGE_VARIABLE == 'australia') {
            env.IMAGE_VARIABLE='AU_IMAGE_TAG'
          }

          env.BUILD_IMAGE = getPreviousSuccessfulBuildImage(BUILD_JOB_NAME)
        }
        println("BUILD_IMAGE: ${env.BUILD_IMAGE}")
      }
    }

    stage('get-deployed-image') {
      steps {
        println("DEPLOY_JOB_NAME: ${DEPLOY_JOB_NAME}")
        script {
          env.DEPLOYED_IMAGE = getCurrentDeployedImage(DEPLOY_JOB_NAME)
        }
        println("DEPLOYED_IMAGE: ${env.DEPLOYED_IMAGE}")
      }
    }

    stage('deploy') {
      when {
        expression { env.BUILD_IMAGE != env.DEPLOYED_IMAGE || env.FORCE_DEPLOY == 'true' || env.RUN_UPDATE == 'true' }
      }
      steps {
        script {
          build(
          job: env.DEPLOY_JOB_NAME,
          wait: true,
          parameters: [
            [$class: 'StringParameterValue', name: 'BUILD_VERSION', value: env.BUILD_IMAGE],
            [$class: 'BooleanParameterValue', name: 'RUN_UPDATE', value: env.RUN_UPDATE]
          ])
          print("Invoke deploy")
        }
      }
    }

    stage('set-build-name') {
      steps {
        buildName("${BRANCH}${env.PREVIOUS_COMMIT_ID == env.CURRENT_COMMIT_ID ? ' (NO CHANGES DETECTED)' : ''}, ${env.BUILD_IMAGE}${env.BUILD_IMAGE == env.DEPLOYED_IMAGE ? ' (ALREADY DEPLOYED)' : ''}")
      }
    }

  }
}
