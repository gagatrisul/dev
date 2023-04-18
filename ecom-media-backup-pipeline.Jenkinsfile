import jenkins.model.*

pipeline {
  agent {
    node {
      label "build-$REGION-gcpops.rodanandfields.com"
    }
  }
  stages {
    stage('pull-repo') {
      steps {
        script {
          def gitVariables = git branch: BRANCH, credentialsId: BITBUCKET_CREDENTIALS_ID, url: 'https://bitbucket.org/rodanandfields/hybris-containers.git'
        }
        sh 'git clean -fdx'
      }
    }
    stage('deploy-media-backup-job') {
      steps {
        sh "cd envs/$ENVIRONMENT/$REGION; ./helm-delete.sh media-backup || true; ./helm-install.sh media-backup"
      }
    }
    stage('trigger-gcloud-db-backup') {
      when {
        expression { ${DB_BACKUP_TOO} == 'true' }
      }
      steps {
        sh "gcloud sql backups create --instance=$DB_INSTANCE"
      }
    }
  }
}
