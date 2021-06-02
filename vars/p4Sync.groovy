// =================================================
// Author: drizzt.xia@dell.com
// Description: P4 Sync
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
        credentialId: checkParam(this, "credentialId", config.credentialId, true),
        workspace: checkParam(this, "workspace", config.workspace, true),
        client: checkParam(this, "client", config.client, true),
        change: checkParam(this, "change", config.change, true),
        force: checkParam(this, "force", config.force, true),
        verbose: true,
    ]
    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        withCredentials([usernamePassword(credentialsId: stepParameters.credentialId, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            def scriptSync = "script/p4-sync.sh"
            def scriptGetLatestChange = "script/p4-get-latest-change.sh"
            sh """
                chmod a+x ${scriptSync} ${scriptGetLatestChange}
                mkdir -p "${stepParameters.workspace}"
                ./${scriptSync} "${stepParameters.port}" "${USERNAME}" "${PASSWORD}" "${stepParameters.client}" "${stepParameters.workspace}" "${stepParameters.change}" "${stepParameters.force}"
                cp -rf ${stepParameters.workspace}/installers/downloads/plugins ${stepParameters.workspace}/mc/lib/
            """
            def clientLatestChangeList = sh(returnStdout: true, script: "./${scriptGetLatestChange} '${stepParameters.port}' '${USERNAME}' '${PASSWORD}' '${stepParameters.client}' '${stepParameters.workspace}' 'client'")
            if (clientLatestChangeList) {
                currentBuild.displayName = "${clientLatestChangeList}".trim()
            }
        }
    }
}

