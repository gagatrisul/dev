package jobs.ecom

import javaposse.jobdsl.dsl.DslFactory

class MaintenancePageBuild {

  public MaintenancePageBuild(
    DslFactory factory,
    String bitbucketCredentialsId,
    String gcrCredentialsKeyFileId
  ) {

    factory.pipelineJob('maintenance-page/maintenance-page-build') {
      logRotator(-1, 60)
      description('Build and push docker images for maintenance pages')
      environmentVariables {
        keepBuildVariables(true)
        keepSystemVariables(true)
        envs([
          BITBUCKET_CREDENTIALS_ID: bitbucketCredentialsId,
          GCR_CREDENTIALS_KEY_FILE_ID: gcrCredentialsKeyFileId
        ])
      }
      parameters {
        stringParam('BRANCH_NAME', 'single-container-for-pages', 'Please Provide Branch name')
      }
      definition {
        cps {
          script('''
pipeline {
  options {
    timeout(time: 15, unit: 'MINUTES')
  }
  agent {
    node {
      label 'docker'
    }
  }
  environment {
    IMAGE_REPOSITORY = 'gcr.io/shared-infra-devops-900362'
  }
  stages {
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
            url: 'https://bitbucket.org/rodanandfields/maintenance-page.git']
          ]
        ])
      }
    }

    stage('clean-workspace') {
      steps {
        sh "git clean -fdx"
      }
    }

    stage('build-and-push') {
      steps {
        script {
          docker.withRegistry('https://gcr.io', "gcr:${GCR_CREDENTIALS_KEY_FILE_ID}") {
            sh """
            docker image build -t ${IMAGE_REPOSITORY}/maintenance-page:latest .
            docker image push ${IMAGE_REPOSITORY}/maintenance-page:latest
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
