// =================================================
// Author: drizzt.xia@dell.com
// Description: Pipeline - Build docker image
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
  - name: docker
    image: hub.xiak.com/avamar/docker:20.10.0
    volumeMounts:
    - name: dockers-volume
      mountPath: /var/run/docker.sock
    command:
    - cat
    tty: true
  volumes:
    - name: dockers-volume
      hostPath:
        path: /var/run/docker.sock
"""
            }
        }
        options {
            buildDiscarder(logRotator(daysToKeepStr: '', numToKeepStr: '20'))
        }
        stages {
            stage('git') {
                steps {
                    container('deploy') {
                        gitCheckout script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('build') {
                steps {
                    container('docker') {
                        containerBuild script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('push') {
                steps {
                    container('docker') {
                        containerPushToRegistry script: parameters.script, config: parameters.config
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