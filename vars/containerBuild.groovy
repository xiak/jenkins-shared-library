// =================================================
// Author: drizzt.xia@dell.com
// Description: Build docker image
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
        stepName: "build-container-image",
        hubDomain: checkParam(this, "hubDomain", config.hubDomain, true),
        hubProject: checkParam(this, "hubProject", config.hubProject, true),
        releaseTag: checkParam(this, "releaseTag", config.releaseTag, true),
        dockerFile: checkParam(this, "dockerFile", config.dockerFile, true),
        workDir: config?.workDir ?: '.',
        verbose: true,
    ]
    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        currentBuild.displayName = stepParameters.releaseTag
        sh "cd ${stepParameters.workDir}; docker build -t ${stepParameters.hubDomain}/${stepParameters.hubProject}/${stepParameters.releaseTag} -f ${stepParameters.dockerFile} . --network=host"
    }
}
