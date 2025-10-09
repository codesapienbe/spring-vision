// Declarative Jenkins pipeline that reuses the project's Makefile.
// - Build (runs `make build`)
// - Test (runs `make build` already runs tests; explicit test stage shown)
// - Deploy (runs `make maven-deploy` when building the main branch and when credentials are provided)
//
// IMPORTANT: Configure these credentials in Jenkins and update the credentialsId values below:
//  - ossrh-creds : Username/Password for Sonatype OSSRH
//  - m2-settings : File credential containing a settings.xml with the OSSRH server credentials
//  - gpg-key     : File credential containing your secret GPG key to import for signing

pipeline {
  agent any
  options {
    timestamps()
    ansiColor('xterm')
  }

  environment {
    # Path where we'll place the settings.xml during the pipeline
    MAVEN_SETTINGS = "${WORKSPACE}/.m2/settings.xml"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build') {
      steps {
        echo "Running full build via Makefile"
        sh 'make build'
      }
    }

    stage('Unit Tests') {
      steps {
        echo "Running tests via Maven (parallel threads)"
        sh 'mvn -T 1C test'
      }
    }

    stage('Prepare Deploy Credentials') {
      when {
        expression { env.BRANCH_NAME == null || env.BRANCH_NAME == '' || env.BRANCH_NAME == 'main' }
      }
      steps {
        echo "Prepare environment for deploy (no secrets are stored in the repo)"
        script {
          // This stage doesn't import secrets by default; Deploy stage will.
          sh 'mkdir -p $(dirname "${MAVEN_SETTINGS}") || true'
        }
      }
    }

    stage('Deploy') {
      when {
        allOf {
          branch 'main'
          not { buildingTag() }
        }
      }
      steps {
        echo "Deploying artifacts (requires Jenkins credentials to be configured)"
        // Credentials usage: please create matching credentials in Jenkins
        withCredentials([
          usernamePassword(credentialsId: 'ossrh-creds', usernameVariable: 'OSSRH_USER', passwordVariable: 'OSSRH_PASS'),
          file(credentialsId: 'm2-settings', variable: 'M2_SETTINGS_FILE'),
          file(credentialsId: 'gpg-key', variable: 'GPG_KEY_FILE')
        ]) {
          // Copy provided settings.xml into place
          sh 'cp "$M2_SETTINGS_FILE" "${MAVEN_SETTINGS}"'

          // Import GPG key into local keyring (non-interactive)
          // Ensure the gpg binary and pinentry are properly configured on the agent
          sh 'gpg --batch --import "$GPG_KEY_FILE" || true'

          // Run the Makefile deploy target (Makefile has safety checks for settings and GPG)
          sh 'make maven-deploy'
        }
      }
    }
  }

  post {
    always {
      echo 'Pipeline finished. Collecting logs/artifacts.'
      archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/*.jar, **/target/*.log, **/target/*.txt'
      junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
    }
    success {
      echo 'Build and deploy succeeded.'
    }
    failure {
      echo 'One or more stages failed. Check console output for details.'
    }
  }
}

