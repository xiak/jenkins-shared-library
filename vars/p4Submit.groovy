// =================================================
// Author: drizzt.xia@dell.com
// Description: P4 Submit
// =================================================


import static com.dell.ap.Precheck.checkScript
import static com.dell.ap.Precheck.checkParam
import com.dell.ap.Avamar

import groovy.transform.Field

@Field String STEP_NAME = getClass().getName()

void call(Map parameters = [:]) {
    final script = checkScript(this, parameters) ?: this
    final config = parameters.config

    // step parameters
    def stepParameters = [
        stepName: "p4-sync",
        // config
        port: checkParam(this, "port", config.port, true),
        user: checkParam(this, "user", config.user, true),
        password: checkParam(this, "password", config.password, true),
        client: checkParam(this, "client", config.client, true),
        clientPath: checkParam(this, "clientPath", config.clientPath, true),
        repoList: checkParam(this, "repoList", config.repoList, true),
        verbose: true,
    ]
    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        sh "mkdir -p ${stepParameters.clientPath}"
        def scriptP4Submit = "script/p4-submit.sh"
        stepParameters.repoList.each { repo ->
            // client path defined in p4 client
            sh "cd ${stepParameters.clientPath} && git clone ${repo}"
        }
        sh """
            chmod a+x ${scriptP4Submit}
            ${scriptP4Submit} \
               "${stepParameters.port}" \
               "${stepParameters.user}" \
               "${stepParameters.password}" \
               "${stepParameters.client}" \
               "${stepParameters.clientPath}"
        """
    }
}

