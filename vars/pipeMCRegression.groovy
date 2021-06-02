// =================================================
// Author: drizzt.xia@dell.com
// Description: Pipeline - Avamar MC Regression test
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
  - name: regression
    image: hub.xiak.com/avamar/deploy:0.0.1-alpine
    imagePullPolicy: IfNotPresent
    command:
    - cat
    tty: true
"""
            }
        }
        stages {
            stage('full-regression') {
                parallel {
                    stage('aui') {
                        when { anyOf { equals expected: 'full-regression', actual: "${parameters.config.type}"; equals expected: 'ui', actual: "${parameters.config.type}"; equals expected: 'aui', actual: "${parameters.config.type}"; } }
                        steps {
                            container('regression') {
                                stagesRun script: parameters.script, config: parameters.config.aui
                            }
                        }
                    }
                    stage('mccli') {
                        when { anyOf { equals expected: 'full-regression', actual: "${parameters.config.type}"; equals expected: 'mccli', actual: "${parameters.config.type}"; } }
                        steps {
                            container('regression') {
                                stagesRun script: parameters.script, config: parameters.config.mccli
                            }
                        }
                    }
                    stage('mcsdk10') {
                        when { anyOf { equals expected: 'full-regression', actual: "${parameters.config.type}"; equals expected: 'mcsdk', actual: "${parameters.config.type}"; equals expected: 'mcsdk10', actual: "${parameters.config.type}"; } }
                        steps {
                            container('regression') {
                                stagesRun script: parameters.script, config: parameters.config.mcsdk10
                            }
                        }
                    }
                    stage('mcsdk20') {
                        when { anyOf { equals expected: 'full-regression', actual: "${parameters.config.type}"; equals expected: 'mcsdk', actual: "${parameters.config.type}"; equals expected: 'mcsdk20', actual: "${parameters.config.type}"; } }
                        steps {
                            container('regression') {
                                stagesRun script: parameters.script, config: parameters.config.mcsdk20
                            }
                        }
                    }
                    stage('restapi') {
                        when { anyOf { equals expected: 'full-regression', actual: "${parameters.config.type}"; equals expected: 'api', actual: "${parameters.config.type}"; equals expected: 'restapi', actual: "${parameters.config.type}"; } }
                        steps {
                            container('regression') {
                                stagesRun script: parameters.script, config: parameters.config.restapi
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


