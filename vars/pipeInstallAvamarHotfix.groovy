// =================================================
// Author: drizzt.xia@dell.com
// Description: Pipeline - Deploy Avamar Hotfix
// =================================================


void call(parameters) {
    pipeline {
        agent {
            kubernetes {
                yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: up
    image: hub.xiak.com/avamar/deploy:0.0.1-alpine
    imagePullPolicy: IfNotPresent
    command:
    - cat
    tty: true
"""
            }
        }
        stages {
            stage('git') {
                steps {
                    container('up') {
                        gitCheckout script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('install') {
                steps {
                    container('up') {
                        avHotfixInstall script: parameters.script, config: parameters.config
                    }
                }
            }
        }
        post {
            /* https://jenkins.io/doc/book/pipeline/syntax/#post */
            success  {
                setPipelineResult(currentBuild)
                sendMail([
                    mimeType: "text/plain",
                    subject: "Avamar Hotfix Install",
                    content: """
Hi Yo,

Hotfix is installed on Avamar server successfuly!
Avamar host     : ${parameters.config.avServer}
Avamar user     : ${parameters.config.avServerUser}
Avamar password : ${parameters.config.avServerPass}
Avamar AUI URL  : https://${parameters.config.avServer}/aui            
Jenkins Link    : ${BUILD_URL} 

By: Avamar Shared CI

""",
                    mailRecipients: parameters.config?.mailRecipients ?: ''
                ])
            }
            aborted  {
                setPipelineResult(currentBuild, 'ABORTED')
                sendMail([
                    mimeType: "text/plain",
                    subject: "Avamar Hotfix Install",
                    content: """
Hi Yo,

Avamar hotfix installation is aborted, please check it manually

Job Link: ${BUILD_URL} 

By: Avamar Shared CI

""",
                    mailRecipients: parameters.config?.mailRecipients ?: ''
                ])
            }
            failure  {
                setPipelineResult(currentBuild, 'FAILURE')
                sendMail([
                    mimeType: "text/plain",
                    subject: "Avamar Hotfix Install",
                    content: """
Hi Yo,

Avamar hotfix installation is failed

${currentBuild.description}

Job Link: ${BUILD_URL} 

By: Avamar Shared CI

""",
                    mailRecipients: parameters.config?.mailRecipients ?: ''
                ])
            }
            unstable {
                setPipelineResult(currentBuild, 'UNSTABLE')
                sendMail([
                    mimeType: "text/plain",
                    subject: "Avamar Hotfix Install",
                    content: """
Hi Yo,

Avamar hotfix inatllation is unstable

${currentBuild.description}

Job Link: ${BUILD_URL}

By: Avamar Shared CI

""",
                    mailRecipients: parameters.config?.mailRecipients ?: ''
                ])
            }
        }
    }
}
