// =================================================
// Author: drizzt.xia@dell.com
// Description: Pipeline - Register Avamar Client
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
  - name: register
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
                    container('register') {
                        gitCheckout script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('register') {
                steps {
                    container('register') {
                        avClientRegister script: parameters.script, config: parameters.config
                    }
                }
            }
        }
        post {
            /* https://jenkins.io/doc/book/pipeline/syntax/#post */
            success  { setPipelineResult(currentBuild) }
            aborted  { setPipelineResult(currentBuild, 'ABORTED') }
            failure  { setPipelineResult(currentBuild, 'FAILURE') }
            unstable { setPipelineResult(currentBuild, 'UNSTABLE') }
        }
    }
}

