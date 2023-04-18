import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import java.lang.Iterable
import java.util.Iterator

env.SOURCE_BUILD = ''
env.TARGET_BUILD = ''
env.SOURCE_JOB_NAME = "ecom/ecom-${env.REGION.toLowerCase()}-${env.SOURCE.toLowerCase()}-deploy"
env.TARGET_JOB_NAME = "ecom/ecom-${env.REGION.toLowerCase()}-${env.TARGET.toLowerCase()}-deploy"

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

def getCurrentDeployedImage(sourceJobName) {
  def builds = new Builds(sourceJobName)
  def build = builds.find{ it -> it.result == 'SUCCESS' }
  def result = build?.buildVariables?.BUILD_VERSION?.toString() ?: ''
  return result
}

def getTargetDeployedImage(targetJobName) {
  def builds = new Builds(targetJobName)
  def build = builds.find{ it -> it.result == 'SUCCESS' }
  def result = build?.buildVariables?.BUILD_VERSION?.toString() ?: ''
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
  
  stage('Get-Source-Build') {
      steps {
        println("SOURCE_JOB_NAME: ${SOURCE_JOB_NAME}")
        script {
          env.SOURCE_BUILD = getCurrentDeployedImage(SOURCE_JOB_NAME)
        }
        println("SOURCE_BUILD: ${env.SOURCE_BUILD}")
      }
    }
  
  stage('Get-Target-Build') {
      steps {
        println("TARGET_JOB_NAME: ${TARGET_JOB_NAME}")
        script {
          env.TARGET_BUILD = getTargetDeployedImage(TARGET_JOB_NAME)
        }
        println("TARGET_BUILD: ${env.TARGET_BUILD}")
      }
    }
  
  stage('Deploy') {
      when {
        expression { env.SOURCE_BUILD != env.TARGET_BUILD }
      }
      steps {
        build(
          job: env.TARGET_JOB_NAME,
          wait: false,
          parameters: [
            [$class: 'StringParameterValue', name: 'BUILD_VERSION', value: env.SOURCE_BUILD]
          ])
      }
    }

    stage('Set-Build-Name') {
      steps {
        buildName("${BRANCH}${env.SOURCE_BUILD == env.TARGET_BUILD ? ' (NO CHANGES DETECTED)' : ''}")
      }
    }
  }
}