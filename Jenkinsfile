#!/usr/bin/env groovy

pipeline {
  agent any
    stages {
      stage("Initialization") {
          when {
              environment name: 'RENAME_BUILDS', value: 'true'
          }
        steps {
          script {
            def version = sh(returnStdout: true, script: 'docker compose run --rm maven mvn -Duser.home=/var/maven help:evaluate -Dexpression=project.version -q -DforceStdout')
            buildName "${env.GIT_BRANCH.replace("origin/", "")}@${version}"
          }
        }
      }
      stage('Build') {
        steps {
          checkout scm
          sh './build.sh $BUILD_SH_EXTRA_PARAM init clean install publish'
        }
      }
    }
}

