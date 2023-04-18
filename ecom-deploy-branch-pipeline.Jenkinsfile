env.PACKAGE_VERSION = ''

import jenkins.model.*

def getDeployJobName(region, environment) {
  return "ecom/ecom-${region.toLowerCase()}-${environment.toLowerCase()}-deploy"
}

def getJob(jobName) {
    def job = Jenkins.instance.getItemByFullName(jobName)
    println(job)
    return job
}

def getDeployVersion(region, environment)
{
  def deploy = getJob( getDeployJobName(env.REGION, env.ENVIRONMENT) )
  def deployBuild = deploy.getLastSuccessfulBuild()
  String ver = deployBuild.buildVariableResolver.resolve("BUILD_VERSION")
  println('Currently deployed version: ' + ver)
  return ver
}

def getLatestHybrisPackageGCR(region, branch) {
  def pattern = 'ecom-' + region + '-' + branch + '.'

  def authenticateCommand = 'google-cloud-sdk/bin/gcloud auth activate-service-account --key-file raf-terraform-gcpops-414436.json'
  def getTagsCommand = """google-cloud-sdk/bin/gcloud container images list-tags gcr.io/shared-infra-devops-900362/hybris --sort-by=~timestamp,tags --filter=${pattern} --limit=1"""

  File dir = new File('/var/jenkins_home/gcloud/')

  def out = new StringBuffer()
  def err = new StringBuffer()
  List envEmpty = []

  Process authenticate = authenticateCommand.execute(envEmpty, dir)
  authenticate.waitFor()

  Process getTags = getTagsCommand.execute(envEmpty, dir)
  getTags.consumeProcessOutput(out, err)
  getTags.waitForProcessOutput()

  def tag = out.toString().readLines()[1].split()[1].split(',').find { it -> it.matches(/${pattern}.*[\d]+/) }

  return tag
}

pipeline {
  agent {
    node {
      label 'master'
    }
  }
  stages {

    stage('get-latest-package') {
      steps {
        script {
          def latestPackageVersion = getLatestHybrisPackageGCR(env.REGION, env.BRANCH)
          println('Latest package version: ' + latestPackageVersion)

          env.PACKAGE_VERSION = latestPackageVersion
        }
      }
    }

    stage('deploy-if-updated') {
      when {
        expression { !getDeployVersion(env.REGION, env.ENVIRONMENT).equalsIgnoreCase(env.PACKAGE_VERSION) }
      }
      steps {
        script {
          println('Can deploy ' + env.PACKAGE_VERSION + ' to ' + env.REGION + ' ' + env.ENVIRONMENT)

          def deploymentJob = getDeployJobName(env.REGION, env.ENVIRONMENT)
          build(
            job: deploymentJob,
            wait:false,
            parameters: [
              [$class: 'StringParameterValue', name: 'BUILD_VERSION', value: env.PACKAGE_VERSION],
              [$class: 'BooleanParameterValue', name: 'RUN_UPDATE', value: env.RUN_UPDATE]
            ])
        }
      }
    }
  }
}
