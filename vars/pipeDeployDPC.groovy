// =================================================
// Author: drizzt.xia@dell.com
// Description: Pipeline - Deploy DPC
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
                    container('deploy') {
                        gitCheckout script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('install') {
                steps {
                    container('deploy') {
                        dpcDeploy script: parameters.script, config: parameters.config
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

DPC server has been installed successfuly!
DPC UI  URL     : https://${parameters.config.ip}
DPC UI  user    : x
DPC UI  password: ${parameters.config.password}
DPC ssh host    : ${parameters.config.ip}
DPC ssh user    : x
DPC ssh password: ${parameters.config.password}

By: Avamar Shared CI
Jenkins Link: ${BUILD_URL} 

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

DPC server is aborted, please check it manually

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

DPC server is failed, please check it manually

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

DPC server is unstable, please check it manually

Job Link: ${BUILD_URL}

By: Avamar Shared CI

""",
                    mailRecipients: parameters.config?.mailRecipients ?: ''
                ])
            }
        }
    }
}
