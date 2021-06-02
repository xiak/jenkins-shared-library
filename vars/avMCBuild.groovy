// =================================================
// Author: drizzt.xia@dell.com
// Description: Build Avamar MC code
// =================================================


import static com.dell.ap.Precheck.checkScript
import static com.dell.ap.Precheck.checkParam

import groovy.transform.Field

@Field String STEP_NAME = getClass().getName()

void call(Map parameters = [:]) {
    final script = checkScript(this, parameters) ?: this
    final config = parameters.config

    // step parameters
    def stepParameters = [
        stepName: "build-mc",
        workspace: checkParam(this, "workspace", config.workspace, true),
        type: checkParam(this, "type", config.type, true),
        verbose: true,
    ]

    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        sh """
            cp -rf ${stepParameters.workspace}/installers/downloads/plugins ${stepParameters.workspace}/mc/lib/
            cd ${stepParameters.workspace}/mc
            export SKIP_EXISTING=true && ant clean-rest-api && ant build-${stepParameters.type}
        """
        if (stepParameters.type == "all") {
            sh "cp -R ${stepParameters.workspace}/mc/gen/ci-tests/reports ."
            junit "reports/*.xml"
        }
    }
}
