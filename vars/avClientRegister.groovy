// =================================================
// Author: drizzt.xia@dell.com
// Description: Register Avamar client to Avamar server
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
        stepName: "avamar-register-client",
        server: checkParam(this, "server", config.server, true),
        client: checkParam(this, "client", config.client, true),
        serverCredentialId: checkParam(this, "serverCredentialId", config.serverCredentialId, true),
        clientCredentialId: checkParam(this, "clientCredentialId", config.clientCredentialId, true),
        domain: config.domain ?: "clients",
        verbose: true,
    ]

    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        withCredentials([usernamePassword(credentialsId: serverCredentialId, usernameVariable: 'SERVER_USER', passwordVariable: 'SERVER_PASSWORD')]) {
            withCredentials([usernamePassword(credentialsId: clientCredentialId, usernameVariable: 'CLIENT_USER', passwordVariable: 'CLIENT_PASSWORD')]) {
                lockResource(resource: "avclient-${stepParameters.client}") {
                    //def av = new Avamar(stepParameters.client, "", "av-_R1_-_R2_.xiak.com")
                    //def avFQDN = av.getFQDN()
                    script.currentBuild.displayName = "${stepParameters.server}"
                    currentBuild.description = "Client: ${stepParameters.client}"

                    // this script must be provided by jenkins pipeline repo
                    def scriptRegister = "script/av-register-client.sh"
                    def cmd = """
                            chmod a+x ${scriptRegister}
                            ./${scriptRegister} "${stepParameters.server}" "${SERVER_USER}" "${SERVER_PASSWORD}" ${stepParameters.client} "${CLIENT_USER}" "${CLIENT_PASSWORD}" "${stepParameters.domain}"
                        """
                    rc = sh(script: cmd, returnStatus: true)
                    if (rc != 0) {
                        script.error("[ERROR] Register client (${stepParameters.client}) to avamar (${stepParameters.server}) failed with code: ${rc}")
                    }
                }
            }
        }
    }
}
