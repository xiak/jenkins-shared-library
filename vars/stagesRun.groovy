// =================================================
// Author: drizzt.xia@dell.com
// Description: Run pipeline stage
// =================================================


import static com.dell.ap.Precheck.checkScript

import groovy.transform.Field

@Field String STEP_NAME = getClass().getName()

void call(Map parameters = [:]) {
    final script = checkScript(this, parameters) ?: this
    final config = parameters.config ?: [:]

    // step parameters
    def stepParameters = [
        stepName: "run-stages",
        stages: config,
        verbose: true,
    ]

    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        stepParameters.stages.collectEntries { jobName, params ->
            stage("${jobName}") {
                build job: "${jobName}", parameters: params
            }
        }
    }
}

