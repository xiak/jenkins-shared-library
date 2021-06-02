// =================================================
// Author: drizzt.xia@dell.com
// Description: Pipeline - Build Avamar MC code
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
  - name: sync
    image: hub.xiak.com/avamar/p4client:0.0.1-ubuntu
    imagePullPolicy: IfNotPresent
    volumeMounts:
    - name: code-volume
      mountPath: "/p4"
    command:
    - cat
    tty: true
  - name: build
    image: hub.xiak.com/avamar/dev:0.0.1-opensuse
    imagePullPolicy: IfNotPresent
    volumeMounts:
    - name: code-volume
      mountPath: "/p4"
    - name: mvn-volume
      mountPath: "/root/.m2/repository"
    - name: rpm-volume
      mountPath: "${parameters.config.rpmPath}"
    - name: npm-cache-volume
      mountPath: "/root/.npm"
    command:
    - cat
    tty: true
  volumes:
    - name: code-volume
      persistentVolumeClaim:
        claimName: pvc-p4-workspace
    - name: mvn-volume
      persistentVolumeClaim:
        claimName: pvc-cache-mvn-${parameters.config.type}
    - name: rpm-volume
      persistentVolumeClaim:
        claimName: pvc-rpm
    - name: npm-cache-volume
      persistentVolumeClaim:
        claimName: pvc-cache-npm
"""
            }
        }
        stages {
            stage('sync') {
                steps {
                    container('sync') {
                        gitCheckout script: parameters.script, config: parameters.config
                        p4Sync script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('git') {
                steps {
                    container('build') {
                        gitCheckout script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('build-code') {
                when { anyOf {equals expected: 'code', actual: "${parameters.config.type}";  equals expected: 'all', actual: "${parameters.config.type}"} }
                steps {
                    container('build') {
                        avMCBuild script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('build-rpm') {
                when { equals expected: 'rpm', actual: "${parameters.config.type}" }
                steps {
                    container('build') {
                        avMCBuildRpm script: parameters.script, config: parameters.config
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