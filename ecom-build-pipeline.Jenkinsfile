env.IMAGE_NAME = 'gcr.io/shared-infra-devops-900362/hybris'
env.CURRENT_COMMIT_ID = ''
env.PREVIOUS_COMMIT_ID = ''
env.US_CHANGED = false
env.JAPAN_CHANGED = false
env.AU_CHANGED = false
env.COMMON_CHANGED = false
env.UNKNOWN_CHANGED = false
env.US_IMAGE_TAG = ''
env.JP_IMAGE_TAG = ''
env.AU_IMAGE_TAG = ''
env.BUILD_NAME = ''

def getPreviousSuccessfulBuild() {
  def build = currentBuild.getPreviousBuild()
  while (build != null) {
    echo "Debug: getPreviousSuccessfulBuild() Previous build: ${build}"
    try {
    def previousBranch = build
      .getRawBuild()
      .getActions()
      .find{ it instanceof ParametersAction }
      .parameters
      .find{ it.name == "BRANCH" }
      .value
    def previousRegion = build
      .getRawBuild()
      .getActions()
      .find{ it instanceof ParametersAction }
      .parameters
      .find{ it.name == "BUILD_REGIONS" }
      .value
    if ((!build.getDisplayName().contains('NO CHANGES DETECTED')) && previousBranch == BRANCH && build.result == 'SUCCESS' && (previousRegion == BUILD_REGIONS || previousRegion == 'ALL')) {
      echo build.getDisplayName()
      echo previousBranch
      echo previousRegion
      echo build.result
      return build
    }
    else {
      build = build.getPreviousBuild()
    }
    }
    catch (error) {
      // Artem. In case this function doesn't work properly return null anyway
      echo "Debug: getPreviousSuccessfulBuild(): ${error}"
      return null
    }
  }
  return null
}

def getPreviousSuccessfulBuildCommitId() {
  def previousBuild = getPreviousSuccessfulBuild();
  if (previousBuild != null) {
    return previousBuild.getBuildVariables()['CURRENT_COMMIT_ID']
  }
  return ''
}

def updateChangeSets(changedFiles) {
  for (file in changedFiles) {
    if (file.startsWith('bin/custom/rodanandfieldsjp/') || file.startsWith('config/JP/')) {
      env.JAPAN_CHANGED = true
    }
    else if (file.startsWith('bin/custom/rodanandfieldsus/') || file.startsWith('config/US/')) {
      env.US_CHANGED = true
      env.AU_CHANGED = true
    }
    else if (file.startsWith('bin/custom/rodanandfields/')) {
      env.COMMON_CHANGED = true
    }
    else {
      env.UNKNOWN_CHANGED = true
    }
  }
}

def buildUS() {
  if ( BUILD_REGIONS != 'ALL' && BUILD_REGIONS != 'US' ){
    return false
  }
  return env.US_CHANGED == 'true' || env.COMMON_CHANGED == 'true' || env.UNKNOWN_CHANGED == 'true' || env.PREVIOUS_COMMIT_ID == '' || FORCE_BUILD == 'true'
}

def buildJapan() {
  if ( BUILD_REGIONS != 'ALL' && BUILD_REGIONS != 'JP' ){
    return false
  }
  return env.JAPAN_CHANGED == 'true' || env.COMMON_CHANGED == 'true' || env.UNKNOWN_CHANGED == 'true' || env.PREVIOUS_COMMIT_ID == '' || FORCE_BUILD == 'true'
}

def buildAU() {
  if ( BUILD_REGIONS != 'ALL' && BUILD_REGIONS != 'AU' ){
    return false
  }
  return env.AU_CHANGED == 'true' || env.COMMON_CHANGED == 'true' || env.UNKNOWN_CHANGED == 'true' || env.PREVIOUS_COMMIT_ID == '' || FORCE_BUILD == 'true'
}

def getTagPrefix(region) {
  return "ecom-${region.toLowerCase()}-${BRANCH.replaceAll('/', '_').trim()}"
}

def getImageTag(region) {
  def buildResult = build job: 'utilities/version-generator', parameters: [[$class: 'StringParameterValue', name: 'PREFIX', value: getTagPrefix(region)]]
  return "${getTagPrefix(region)}.${buildResult.getBuildVariables()['BUILD_VERSION'].trim()}"
}

def sendSlack(message, channel = "ci-cd", color = "0000FF") {
  def tokenCredentialId = '98d15074-0478-4200-8349-04307a0cba16'
  def teamDomain = 'rodanandfields'
  def result = slackSend channel: "${channel}", color: "${color}", message: "${message}", teamDomain: "${teamDomain}", tokenCredentialId: "${tokenCredentialId}"
  return result
}

def getUserName() {
  def specificCause = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause')
  if (specificCause) {
    return "by ${specificCause.userName}"
  }
  return ''
}

// Main pipeline
pipeline {
  /*
  * Run everything on an existing agent configured with a label 'docker'.
  * This agent will need docker, git and a jdk installed at a minimum.
  */
  agent {
    node {
      label 'ecom-builder'
    }
  }

  // all pipeline stages
  stages {

    stage('pull-repo') {
      steps {
        script {
          def gitVariables = git branch: "${BRANCH}", credentialsId: BITBUCKET_CREDENTIALS_ID, url: 'https://bitbucket.org/rodanandfields/ecom-atlas.git'
          env.CURRENT_COMMIT_ID = gitVariables.GIT_COMMIT

          def userName = getUserName()
          sendSlack("${env.JOB_NAME} - #${env.BUILD_NUMBER} ${BRANCH} (${env.CURRENT_COMMIT_ID}) Started ${userName} ${BUILD_URL}")
        }
        lastChanges format:'SIDE',matching: 'LINE', specificRevision: env.PREVIOUS_COMMIT_ID
        sh 'git clean -fdx'
      }
    }

    stage('determine-regions-to-build') {
      steps {
        script {
          env.PREVIOUS_COMMIT_ID = getPreviousSuccessfulBuildCommitId()
          if (env.PREVIOUS_COMMIT_ID != '') {
            def changedFiles = sh(returnStdout: true, script: "git diff $env.PREVIOUS_COMMIT_ID $env.CURRENT_COMMIT_ID --name-only | uniq | sort").split()
            updateChangeSets(changedFiles)
          }
        }
        echo "US Changed: ${env.US_CHANGED}\nJapan Changed: ${env.JAPAN_CHANGED}\nAustralia Changed: ${env.AU_CHANGED}\nCommon Changed: ${env.COMMON_CHANGED}\nUnknown Changed: ${env.UNKNOWN_CHANGED}\nPrevious Commit Id: ${env.PREVIOUS_COMMIT_ID}"
      }
    }

    stage('generate-versions') {
      when {
        expression { buildUS() || buildJapan() || buildAU() }
      }
      steps {
        script {
          if (buildUS()) {
            env.US_IMAGE_TAG = getImageTag('us')
            print("US Tag: ${env.US_IMAGE_TAG}")
          }
          if (buildJapan()) {
            env.JP_IMAGE_TAG = getImageTag('jp')
            print("Japan Tag: ${env.JP_IMAGE_TAG}")
          }
          if (buildAU()) {
            env.AU_IMAGE_TAG = getImageTag('au')
            print("AU Tag: ${env.AU_IMAGE_TAG}")
          }
        }
      }
    }

    stage('set-build-name') {
      steps {
        script {
          def buildName = ''
          if (buildUS()) {
            buildName = "${env.US_IMAGE_TAG}"
          }
          if (buildJapan()) {
            if (buildName != '') {
              buildName += ', '
            }
            buildName += env.JP_IMAGE_TAG
          }
          if (buildAU()) {
            if (buildName != '') {
              buildName += ', '
            }
            buildName += env.AU_IMAGE_TAG
          }
          if (buildName == '') {
            buildName = "${BRANCH} (NO CHANGES DETECTED)"
          }
          print("Build Name: ${buildName}")
          currentBuild.setDisplayName(buildName)
          env.BUILD_NAME = buildName
        }
      }
    }

    stage('build-hybris-base-image') {
        when {
          expression { buildUS() || buildJapan() || buildAU() }
        }
        steps {
          sh script: 'docker image build --pull --network host --target hybris-source -t "hybris:hybris-source"  .'
        }
    }

    stage('build-regional-hybris-images'){
      parallel {
        stage('us') {
          when {
            expression { buildUS() }
          }
          stages {
            stage('build-hybris-image') {
              steps {
                sh script: "./build.sh ${env.US_IMAGE_TAG} US"
              }
            }
            stage('Push git tag') {
              steps {
                script {
                  // add build-id as git tag and push back to the repo
                  // there is an open issue to allow to do it with GitPublisher plugin https://issues.jenkins-ci.org/browse/JENKINS-28335
                  withCredentials([usernamePassword(
                  credentialsId: BITBUCKET_CREDENTIALS_ID,
                  usernameVariable: 'GIT_USERNAME',
                  passwordVariable: 'GIT_PASSWORD')]) {
                  sh """git config --local credential.helper "!p() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; p"
                        git config user.email "sys-jenkins@rodanandfields.com"
                        git config user.name "sys-jenkins"
                        git tag -a ${env.US_IMAGE_TAG} -m 'Jenkins'; git push origin ${env.US_IMAGE_TAG}"""
                  }
                }
              }
            }
            stage('sonar') {
              when {
                expression { SKIP_SONAR_SCAN == 'false' }
              }
              steps {
                build(
                  job: 'ecom/ecom-sonar',
                  wait:false,
                  parameters: [
                    [$class: 'StringParameterValue', name: 'BRANCH_NAME', value: env.CURRENT_COMMIT_ID],
                    [$class: 'StringParameterValue', name: 'BUILD_VERSION', value: env.US_IMAGE_TAG],
                    [$class: 'StringParameterValue', name: 'REGION', value: 'us']
                  ])
              }
            }
          }
        }
        stage('jp') {
          when {
            expression { buildJapan() }
          }
          stages() {
            stage('build-hybris-image') {
              steps {
                sh script: "./build.sh ${env.JP_IMAGE_TAG} JP"
              }
            }
            stage('Push git tag') {
              steps {
                script {
                  withCredentials([usernamePassword(
                  credentialsId: BITBUCKET_CREDENTIALS_ID,
                  usernameVariable: 'GIT_USERNAME',
                  passwordVariable: 'GIT_PASSWORD')]) {
                  sh """git config --local credential.helper "!p() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; p"
                        git config user.email "sys-jenkins@rodanandfields.com"
                        git config user.name "sys-jenkins"
                        git tag -a ${env.JP_IMAGE_TAG} -m 'Jenkins'; git push origin ${env.JP_IMAGE_TAG}"""
                  }
                }
              }
            }
            stage('sonar') {
              when {
                expression { SKIP_SONAR_SCAN == 'false' }
              }
              steps {
                build(
                  job: 'ecom/ecom-sonar',
                  wait:false,
                  parameters: [
                    [$class: 'StringParameterValue', name: 'BRANCH_NAME', value: env.CURRENT_COMMIT_ID],
                    [$class: 'StringParameterValue', name: 'BUILD_VERSION', value: env.JP_IMAGE_TAG],
                    [$class: 'StringParameterValue', name: 'REGION', value: 'jp']
                  ])
              }
            }
          }
        }
        stage('au') {
          when {
            expression { buildAU() }
          }
          stages() {
            stage('build-hybris-image') {
              steps {
                sh script: "./build.sh ${env.AU_IMAGE_TAG} AU"
              }
            }
            stage('Push git tag') {
              steps {
                script {
                  withCredentials([usernamePassword(
                  credentialsId: BITBUCKET_CREDENTIALS_ID,
                  usernameVariable: 'GIT_USERNAME',
                  passwordVariable: 'GIT_PASSWORD')]) {
                  sh """git config --local credential.helper "!p() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; p"
                        git config user.email "sys-jenkins@rodanandfields.com"
                        git config user.name "sys-jenkins"
                        git tag -a ${env.AU_IMAGE_TAG} -m 'Jenkins'; git push origin ${env.AU_IMAGE_TAG}"""
                  }
                }
              }
            }
            stage('sonar') {
              when {
                expression { SKIP_SONAR_SCAN == 'false' }
              }
              steps {
                build(
                  job: 'ecom/ecom-sonar',
                  wait:false,
                  parameters: [
                    [$class: 'StringParameterValue', name: 'BRANCH_NAME', value: env.CURRENT_COMMIT_ID],
                    [$class: 'StringParameterValue', name: 'BUILD_VERSION', value: env.AU_IMAGE_TAG],
                    [$class: 'StringParameterValue', name: 'REGION', value: 'au']
                  ])
              }
            }
          }
        }
      }
    }
  }

  post {
    failure {
      sendSlack("${env.JOB_NAME} - #${env.BUILD_NUMBER} - ${env.BUILD_NAME} Failed after ${currentBuild.durationString.replace(' and counting', '')}", "ci-cd", "#FF0000")

      build(
        job: 'utilities/email-deployment-status',
        wait:false,
        parameters: [
          [$class: 'StringParameterValue', name: 'SUBJECT', value: currentBuild.getDisplayName().trim()],
          [$class: 'StringParameterValue', name: 'BUILD_PROJECT_NAME', value: 'ecom/ecom-build'],
          [$class: 'StringParameterValue', name: 'BUILD_BUILD_VERSION', value: currentBuild.getDisplayName().trim()],
          [$class: 'StringParameterValue', name: 'EMAIL_RECIPIENTS', value: EMAIL_RECIPIENTS]
        ])
    }
    success {
      sendSlack("${env.JOB_NAME} - #${env.BUILD_NUMBER} - ${env.BUILD_NAME} Success after ${currentBuild.durationString.replace(' and counting', '')}", "ci-cd", "#00FF00")
    }
  }
}
