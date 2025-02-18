
pipeline {

  agent any

  parameters {
        booleanParam(name: 'TRIGGER_RELEASE', defaultValue: false, description: 'Trigger a Maven release')
  }

  tools {
      maven 'mvn-3.9.9'
      jdk 'jdk17'
  }

  options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: "30"))
        timestamps()
  }

  stages {

    stage("Build") {
      when {
        expression { params.TRIGGER_RELEASE == false }
      }
      steps {
        withMaven(globalMavenSettingsConfig: 'maven-settings-nexus-internal-ci-build-jobs', jdk: 'jdk17', maven: 'mvn-3.9.9') {
          sh "mvn clean install --batch-mode"
        }
      }
    }

    stage("Release") {

      when {
        branch 'master'
        expression { params.TRIGGER_RELEASE == true }
      }

      steps {
        script {

          pom = readMavenPom file: 'pom.xml'
          def releaseVersion = pom.version.replaceAll('-SNAPSHOT', "")
          currentBuild.description = "Release: ${releaseVersion}"

          deleteDir()
          checkout scm
          sh "git checkout $BRANCH_NAME"

          withMaven(globalMavenSettingsConfig: 'maven-settings-nexus-internal-ci-build-jobs', jdk: 'jdk17', maven: 'mvn-3.9.9') {
            sshagent(['jenkins-ops.github-organizations-plugin']) {
              sh "mvn release:clean release:prepare release:perform --batch-mode"
            }
          }

        }
      }
    }

  }
}
