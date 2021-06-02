// =================================================
// Author: drizzt.xia@dell.com
// Description: Pipeline - The yaml interface for all pipeline
// =================================================


import com.dell.ap.ProjectConfig
import com.dell.ap.parser.ConfigParser

void call(parameters) {
    pipeline {
        agent {
            kubernetes {
                yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: pipe
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
                    container('pipe') {
                        gitCheckout script: parameters.script, config: parameters.config
                    }
                }
            }
            stage('dynamic-stages') {
                steps {
                    container('pipe') {
                        script {
                            def yaml
                            if (parameters.config.yaml) {
                                yaml = readYaml text: parameters.config.yaml
                            } else if (parameters.config.configFile) {
                                yaml = readYaml file: parameters.config.configFile
                            } else {
                                this.error("[ERROR] Param missing: You must provide either yaml text or yaml file path")
                            }

                            ProjectConfig config = ConfigParser.parse(yaml, env)
                            if (config.name) {
                                currentBuild.displayName = config.name
                            }
                            if (config.description) {
                                currentBuild.description = config.description
                            }
                            def closure = stepsRun(config)
                            closure([:])
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



