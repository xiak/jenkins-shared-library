// =================================================
// Author: drizzt.xia@dell.com
// Description: Pipeline - Deploy VM to an ESXI of VCenter
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
  - name: deploy
    image: hub.xiak.com/avamar/deploy:0.0.2-alpine
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
                    container('deploy') {
                        gitCheckout script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('install') {
                steps {
                    container('deploy') {
                        vmDeploy script: parameters.script, config: parameters.config
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
                    subject: "Avamar Installation",
                    content: """
Hi Yo,

VM has been installed successfuly!
Host     : ${parameters.config.ip}
User     : x
Password : ${parameters.config.password}

By: Avamar Shared CI
Jenkins Link : ${BUILD_URL}

""",
                    mailRecipients: parameters.config?.mailRecipients ?: ''
                ])
            }
            aborted  {
                setPipelineResult(currentBuild, 'ABORTED')
                sendMail([
                    mimeType: "text/plain",
                    subject: "Avamar Installation",
                    content: """
Hi Yo,

Avamar client installation is aborted, please check it manually

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
                    subject: "Avamar Installation",
                    content: """
Hi Yo,

Avamar client installation is failed, please check it manually

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
                    subject: "Avamar Installation",
                    content: """
Hi Yo,

Avamar client installation is unstable, please check it manually

Job Link: ${BUILD_URL}

By: Avamar Shared CI

""",
                    mailRecipients: parameters.config?.mailRecipients ?: ''
                ])
            }
        }
    }
}
