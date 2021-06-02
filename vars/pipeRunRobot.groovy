// =================================================
// Author: drizzt.xia@dell.com
// Description: Pipeline - Robotframework Test Interface
// =================================================


void call(parameters) {
    def python = parameters.config.python ?: 'python3'
    def placeholderVolumeMount = parameters.config?.qadepot ? """
    - name: qadepot-volume
      mountPath: "${parameters.config?.qadepot}"
""" : ""
    def placeholderVolume = parameters.config?.qadepot ? """
  - name: qadepot-volume
    persistentVolumeClaim:
      claimName: pvc-nfs
""" : ""

    pipeline {
        agent {
            kubernetes {
                yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: robot
    image: hub.xiak.com/avamar/robot:0.0.1-${python}
    imagePullPolicy: Always
    volumeMounts:
    - name: logs-volume
      mountPath: "${parameters.config.logPath}"
${placeholderVolumeMount}
    command:
    - cat
    tty: true
  volumes:
  - name: logs-volume
    persistentVolumeClaim:
      claimName: pvc-test-log
${placeholderVolume} 
"""
            }
        }
        stages {
            stage('git') {
                when { not { equals expected: 'p4', actual: "${parameters.config.smcType}" } }
                steps {
                    container('robot') {
                        gitCheckout script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('p4') {
                when { equals expected: 'p4', actual: "${parameters.config.smcType}" }
                steps {
                    container('robot') {
                        p4Checkout script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('robot-test') {
                when { equals expected: 'robot', actual: "${parameters.config.type}" }
                steps {
                    container('robot') {
                        robotRun script: parameters.script, config: parameters.config
                    }
                }
                post {
                    always {
                        script {
                            if (env.isRobotTask == "true") {
                                try {
                                    robot outputPath: pwd(), passThreshold: 100.0, unstableThreshold: 90.0
                                } catch(Exception err) {
                                    echo "${err}"
                                }
                            }
                        }
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

