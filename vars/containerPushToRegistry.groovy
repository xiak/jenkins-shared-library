// =================================================
// Author: drizzt.xia@dell.com
// Description: Push docker image to the registry
// =================================================


import static com.dell.ap.Precheck.checkScript
import static com.dell.ap.Precheck.checkParam

import groovy.transform.Field

@Field String STEP_NAME = getClass().getName()

void call(Map parameters = [:]) {
    final script = checkScript(this, parameters) ?: this
    final config = parameters.config

    // parameters
    def stepParameters = [
        stepName: "push-container-image",
        hubDomain: checkParam(this, "hubDomain", config.hubDomain, true),
        hubProject: checkParam(this, "hubProject", config.hubProject, true),
        hubCredentialId: checkParam(this, "hubCredentialId", config.hubCredentialId, true),
        releaseTag: checkParam(this, "releaseTag", config.releaseTag, true),
        verbose: true,
    ]
    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        // https://jenkins.io/doc/pipeline/steps/credentials-binding
        withCredentials([usernamePassword(credentialsId: stepParameters.hubCredentialId, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            sh """
                docker login -u ${USERNAME} -p ${PASSWORD} ${stepParameters.hubDomain}
                docker push ${stepParameters.hubDomain}/${stepParameters.hubProject}/${stepParameters.releaseTag}
            """
        }
    }
}
