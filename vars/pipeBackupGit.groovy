// =================================================
// Author: drizzt.xia@dell.com
// Description: Pipeline - Backup data from git to p4
// =================================================


void call(parameters) {
    // backup git to p4
    pipeline {
        agent {
            kubernetes {
                yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: p4
    image: hub.xiak.com/avamar/p4client:0.0.1-cli
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
                    container('p4') {
                        gitCheckout script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('backup') {
                steps {
                    container('p4') {
                        p4Submit script: parameters.script, config: parameters.config
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
                    subject: "Git Backup",
                    content: """
Hi Yo,

Git repos have been backuped to p4 
       
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

Git backup is aborted, please check it manually

By: Avamar Shared CI
Job Link: ${BUILD_URL}

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

Git backup is failed, please check it manually

By: Avamar Shared CI
Job Link: ${BUILD_URL}

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

Git backup is unstable, please check it manually

By: Avamar Shared CI
Job Link: ${BUILD_URL}
""",
                    mailRecipients: parameters.config?.mailRecipients ?: ''
                ])
            }
        }
    }
}

