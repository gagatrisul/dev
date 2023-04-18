env.IMAGE_NAME = 'gcr.io/shared-infra-devops-900362/hybris'
env.BRANCH = 'develop'

// Main pipeline
pipeline {
  agent {
    node {
      label 'deploy*'
    }
  }
  stages {

    stage('pull-repo') {
      when {
        expression { GIT_TAG_CLEANUP == 'true' }
      }
      steps {
        script {
          def gitVariables = git branch: "${BRANCH}", credentialsId: BITBUCKET_CREDENTIALS_ID, url: 'https://bitbucket.org/rodanandfields/ecom-atlas.git'
          env.CURRENT_COMMIT_ID = gitVariables.GIT_COMMIT
        }
        lastChanges format:'SIDE',matching: 'LINE', specificRevision: env.PREVIOUS_COMMIT_ID
        sh 'git clean -fdx'
      }
    }

    stage('Cleanup Git tags') {
      when {
        expression { GIT_TAG_CLEANUP == 'true' }
      }
      steps {
        script {
        withCredentials([usernamePassword(
        credentialsId: BITBUCKET_CREDENTIALS_ID,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD')]) {
        sh """git config --local credential.helper "!p() { echo username=\\$GIT_USERNAME; echo password=\\$GIT_PASSWORD; }; p"
              git config user.email "sys-jenkins@rodanandfields.com"
              git config user.name "sys-jenkins"

              # Remove git ecom-* tags older than 6 months
              all_tags_array=()
              older_date=`date -d "6 months ago" +%s`
              for tag in `git tag -l ecom* --format='%(refname:short)'`; do \
                tag_date=$(date -d `git tag -l $tag --format='%(creatordate:short)'` +%s); \
                if [[ $tag_date -lt $older_date ]]; then \
                  git push origin :$tag; \
                fi; \
              done"""
        }
        }
      }
    }

    stage('Cleanup GCR images') {
      when {
        expression { DOCKER_IMAGE_CLEANUP == 'true' }
      }
      steps {
        script {
        sh """docker_registry=$IMAGE_NAME

              # Remove all tagless GCR images
              for digest in `gcloud container images list-tags ${docker_registry} --filter "NOT tags:*" --format='get(digest)'`; do \
                gcloud container images delete ${docker_registry}\@${digest} --quiet; \
              done

              # Remove GCR images older than 6 months
              for old_image_digest in `gcloud container images list-tags ${docker_registry} --filter "tags:*" --format "get(digest)" --filter="timestamp.datetime<-P6M"`; do \
                gcloud container images delete ${docker_registry}\@${old_image_digest} --force-delete-tags --quiet; \
              done"""
        }
      }
    }
  }
}
