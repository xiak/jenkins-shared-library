// =================================================
// Author: drizzt.xia@dell.com
// Description: SCM - git
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
        stepName: "scm-git",
        url: checkParam(this, "gitUrl", config.gitUrl, true),
        branch: checkParam(this, "gitBranch", config.gitBranch, true),
        credentialsId: checkParam(this, "gitCredentialId", config.gitCredentialId, true),
        changelog: ("${config?.changelog}" == "true"),
        poll: ("${config?.poll}" == "true"),
        verbose: true,
    ]

    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        git url: stepParameters.url,
            branch: stepParameters.branch,
            credentialsId: stepParameters.credentialsId,
            changelog: stepParameters.changelog,
            poll: stepParameters.poll
    }

}
