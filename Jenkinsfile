#!/usr/bin/env groovy

pipeline {
  agent any
    stages {
      stage('Build') {
        steps {
          checkout scm
          sh './build.sh $BUILD_SH_EXTRA_PARAM clean install publish'
        }
      }
    }
}

