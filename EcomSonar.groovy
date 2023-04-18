package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory

class EcomSonar {

  public EcomSonar(
    DslFactory factory,
    String bitbucketCredentialsId,
    String gcrCredentialsKeyFileId
  ) {

    factory.pipelineJob('ecom/ecom-sonar') {
      logRotator(-1, 60)
      description('Sonar scan in docker')
      environmentVariables {
        keepBuildVariables(true)
        keepSystemVariables(true)
        envs([
          BITBUCKET_CREDENTIALS_ID: bitbucketCredentialsId,
          GCR_CREDENTIALS_KEY_FILE_ID: gcrCredentialsKeyFileId
        ])
      }
      parameters {
        stringParam('BRANCH_NAME', 'develop', 'Build branch')
        stringParam('BUILD_VERSION', '', 'Build version to scan')
        choiceParam('REGION', ['jp', 'us', 'au'], 'For region-specific files included in the build.')
      }
      definition {
        cps {
          script('''
pipeline {
  options {
    timeout(time: 300, unit: 'MINUTES')
  }
  /*
  * Run everything on an existing agent configured with a label 'docker'.
  * This agent will need docker, git and a jdk installed at a minimum.
  */
  agent {
    node {
      label "deploy-\${REGION}"
    }
  }

  stages {
    stage('set-build-name') {
      steps {
        script {
          currentBuild.setDisplayName("#${BUILD_ID}.${BUILD_VERSION}")
        }
      }
    }

    stage('pull-repo') {
      steps {
        checkout([
          $class: 'GitSCM',
          branches: [[name: "${BRANCH_NAME}"]],
          doGenerateSubmoduleConfigurations: false,
          extensions: [],
          submoduleCfg: [],
          userRemoteConfigs: [[
            credentialsId: BITBUCKET_CREDENTIALS_ID,
            url: 'https://bitbucket.org/rodanandfields/ecom-atlas.git']
          ]
        ])
      }
    }

    stage('clean-workspace') {
      steps {
        sh "git clean -fdx"
      }
    }

    stage('sonar-qube') {
      steps {
        script {
          docker.withRegistry('https://gcr.io', "gcr:${GCR_CREDENTIALS_KEY_FILE_ID}") {
            sh """
            ./sonar/run-scan.sh
            """
          }
        }
      }
    }
  }
}
''')
        }
      }
    }
  }
}
