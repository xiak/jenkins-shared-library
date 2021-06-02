// =================================================
// Author: drizzt.xia@dell.com
// Description: Pipeline - Test Trigger
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
  - name: trigger
    image: hub.xiak.com/avamar/deploy:0.0.1-alpine
    imagePullPolicy: IfNotPresent
    command:
    - cat
    tty: true
    volumeMounts:
    - name: marker-path
      mountPath: "${parameters.config.markerPath}"
  volumes:
  - name: marker-path
    persistentVolumeClaim:
      claimName: pvc-cache-marker
"""
            }
        }
        stages {
            stage('git') {
                steps {
                    container('trigger') {
                        gitCheckout script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('trigger') {
                steps {
                    container('trigger') {
                        testTrigger script: parameters.script, config: parameters.config
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

