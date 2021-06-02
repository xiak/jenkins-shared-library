// =================================================
// Author: drizzt.xia@dell.com
// Description: Deploy Avamar client
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
        stepName: "install-ave",
        // config
        url: checkParam(this, "url", config.url, true),
        ip: checkParam(this, "ip", config.ip, true),
        user: checkParam(this, "user", config.user, true),
        password: checkParam(this, "password", config.password, true),
        osType: checkParam(this, "osType", config.osType, true),
        verbose: true,
    ]
    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        echo "Install Avamar client on ${stepParameters.osType} host ${stepParameters.ip}"
        currentBuild.displayName = stepParameters.ip
        currentBuild.description = "${stepParameters.osType} client"
        lockResource(resource: "avclient-${stepParameters.ip}") {
            // this script must be defined in jenkins pipeline repo
            def scriptAvClientInstall = "script/av-client-install.sh"
            sh """
                chmod +x ${scriptAvClientInstall}
                # 安装 Avamar client
                ./${scriptAvClientInstall} \\
                    "${stepParameters.ip}" \\
                    "${stepParameters.user}" \\
                    "${stepParameters.password}" \\
                    "${stepParameters.osType}" \\
                    "${stepParameters.url}"
            """
        }
    }
}
