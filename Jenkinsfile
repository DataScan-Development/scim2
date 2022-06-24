pipeline {
    agent any
    environment {
        NEXUS_URL   = 'https://nexus.onedatascan.io/nexus/content/repositories/thirdparty/'
        NEXUS_CREDS = credentials('jenkins-maven')
    }
    parameters{
        string(defaultValue: '#product_dev', description: 'The space delimited list of channels to publish slack notifications.', name: 'SLACK_CHANNELS')
    }
    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    stages {
        stage('setup') {
            steps {
                script {
                    currentBuild.displayName = "bettercloud-scim2-library"
                    currentBuild.description = "Pipeline Build of bettercloud-scim2-library"

                    publishAllTheSlacks("STARTED - ${currentBuild.description}", params.SLACK_CHANNELS)
                }
            }
        }
        stage('gradle') {
            agent {
                docker {
                    image 'gradle:7.4.2-jdk8'
                    reuseNode true
                }
            }
            stages {
                stage('build') {
                    steps {
                        sh "gradle clean build -x test"
                    }
                }
                stage('deploy') {
                    steps {
                        sh "gradle publish"
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                deleteDir()
            }
        }
        success {
            script {
                publishAllTheSlacks("SUCCESS - ${currentBuild.description}", params.SLACK_CHANNELS)
            }
        }
        failure {
            script {
                currentBuild.result = "FAILED"
                publishAllTheSlacks("FAILED: Job '${env.JOB_NAME} Build ${env.BUILD_NUMBER}'", params.SLACK_CHANNELS)
            }
        }
    }
}

@NonCPS
def publishAllTheSlacks(msg, channels) {
    slackSend (
        channel: channels,
        message: msg,
        teamDomain: 'onedatascan'
    )
}