env.CURRENT_COMMIT_ID = ''

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

pipeline {

  agent {
    node {
      label 'ecom-builder'
    }
  }
  stages {

  stage('cleanup') {
        steps {
            cleanWs()
        }
    }
    stage('Pull repo') {
      steps {
        withCredentials([
          string(credentialsId: '76a796f0-3427-4ee1-bb3d-f438c847c6b2', variable: 'VAULT_PASS'),
        ]) {
        script {
          def gitVariables = git branch: "${BRANCH}", credentialsId: BITBUCKET_CREDENTIALS_ID, url: 'https://bitbucket.org/rodanandfields/e-commerce.git'
          env.CURRENT_COMMIT_ID = gitVariables.GIT_COMMIT

          def userName = getUserName()
          sendSlack("${env.JOB_NAME} - #${env.BUILD_NUMBER} ${BRANCH} (${env.CURRENT_COMMIT_ID}) Started ${userName} ${BUILD_URL}")
        }
        sh script: """echo ${VAULT_PASS} > pwd.txt
                      ansible-vault decrypt config/secret.properties.au.dev3 config/secret.properties.au.tst4 config/secret.properties.au.tst6 config/secret.properties.au.ppd3 config/secret.properties.au.prd --vault-password-file=pwd.txt
                      cat config/secret.properties.au.dev3 >> config/local-specific.properties.au.dev3
                      cat config/secret.properties.au.tst4 >> config/local-specific.properties.au.tst4
                      cat config/secret.properties.au.tst6 >> config/local-specific.properties.au.tst6
                      cat config/secret.properties.au.ppd3 >> config/local-specific.properties.au.ppd3
                      cat config/secret.properties.au.prd >> config/local-specific.properties.au.prd
                      rm -f pwd.txt"""
        }
      }
    }
    stage('Download from artifactory') {
        steps {
            rtDownload (
                serverId: 'ARTIFACTORY',
                    spec: """{
                        "files": [
                            {
                            "pattern": "rf-snapshot-local/Hybris-5.5.0.0/",
                            "target": "${WORKSPACE}/",
                            "flat": "true"
                            }
                        ]
                    }"""
                )
            }
    }
    stage('Unzip artifacts') {
      steps {
        sh 'unzip -q Hybris-5.5.0.0.zip -d ./build'
        sh 'unzip HybrisConfig.zip -d ./HybrisConfig'
        sh 'mv bin/ext-rodanandfields build/Hybris-5.5.0.0/bin/ext-rodanandfields'
        sh 'mv config build/Hybris-5.5.0.0'
      }
    }
    stage ('Set build name') {
      steps {
        script {
          def buildVersion = build job: 'utilities/version-generator', parameters: [[$class: 'StringParameterValue', name: 'PREFIX', value: "${BRANCH}"]]
          version = "${buildVersion.getBuildVariables()['BUILD_VERSION'].trim()}"
          echo "$version"
        }
      }
    }
    stage('Build') {
      steps {
        script {
          def gitVariables = git branch: 'master', credentialsId: BITBUCKET_CREDENTIALS_ID, url: "https://bitbucket.org/rodanandfields/devops-scripts.git"
        }
        script {
          buildName = "ecom-${BRANCH}.${version}"
          print("Build Name: ${buildName}")
          currentBuild.setDisplayName(buildName)
        }
		sh script: "docker build -t hybris:${buildName} -f ./hybris/DockerCI/hybrisbuilder.Dockerfile ."
        sh script: """id=\$(docker create hybris:${buildName})
                      docker cp \$id:/home/hybris/Hybris-5.5.0.0/temp ${WORKSPACE}/build/Hybris-5.5.0.0/
                      docker rm -v \$id"""
      }
    }
    stage('Upload to artifactory') {
        steps {
            rtUpload (
                serverId: 'ARTIFACTORY',
                    spec: """{
                        "files": [
                            {
                            "pattern": "${WORKSPACE}/build/Hybris-5.5.0.0/temp/hybris/hybrisServer/*.zip",
                            "target": "rf-snapshot-local/ecom/${buildName}/"
                            }
                        ]
                    }"""
                )
        }
    }
    stage ('Deploy') {
      steps{
        script {
          cleanWs()
          if (env.DEPLOY_ENV != 'NONE') {
            build job: "ecom/ecom-au-${DEPLOY_ENV}-deploy", parameters: [string(name: 'BUILD_VERSION', value:"${buildName}")], wait:false
            script {
              sendSlack("${buildName}: ${env.DEPLOY_ENV} deployment Triggered")
            }
          }
        }
      }
    }
  }
  post {
    failure {
      sendSlack("${env.JOB_NAME} - #${env.BUILD_NUMBER} - ${buildName} Failed after ${currentBuild.durationString}", "ci-cd", "#FF0000")
    }
    success {
      sendSlack("${env.JOB_NAME} - #${env.BUILD_NUMBER} - ${buildName} Success after ${currentBuild.durationString}", "ci-cd", "#00FF00")
    }
  }
}
