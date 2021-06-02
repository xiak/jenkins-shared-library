// =================================================
// Author: drizzt.xia@dell.com
// Description: Deploy DPC
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
        stepName: "install-dpc",
        // config
        version: checkParam(this, "version", config.version, true),
        ip: checkParam(this, "ip", config.ip, true),
        password: checkParam(this, "password", config.password, true),
        gateway: checkParam(this, "gateway", config.gateway, true),
        netmask: checkParam(this, "netmask", config.netmask, true),
        dns: checkParam(this, "dns", config.dns, true),
        fqdn: checkParam(this, "dns", config.fqdn, true),
        ntp: checkParam(this, "ntp", config.ntp, true),
        timezone: checkParam(this, "timezone", config.timezone, true),
        esxiGateway: checkParam(this, "esxiGateway", config.esxiGateway, true),
        verbose: true,
    ]
    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        echo "Install DPC ${stepParameters.version}, ip: ${stepParameters.ip}"
        currentBuild.displayName = stepParameters.ip
        currentBuild.description = "${stepParameters.version}"
        lockResource(resource: "dpc-${stepParameters.ip}") {
            if (!stepParameters.version) {
                script.error("[ERROR] - you must provide DPC version")
            }
            def ovaUrl = "http://xiak.com/dpc/artifactory/public/com/emc/dpsg/dpc/emc-dpc-ova/${stepParameters.version}/emc-dpc-ova-${stepParameters.version}.ova"
            def scriptDpcInstall = "script/dpc-install.sh"
            def vmName = "dpc.${stepParameters.ip}"
            sh """
                chmod +x ${scriptDpcInstall}
                # 安装 DPC Server
                ./${scriptDpcInstall} \
                    "${stepParameters.ip}" \
                    "${stepParameters.gateway}" \
                    "${stepParameters.netmask}" \
                    "${stepParameters.dns}" \
                    "${stepParameters.fqdn}" \
                    "${stepParameters.ntp}" \
                    "${stepParameters.timezone}" \
                    "${stepParameters.password}" \
                    "${vmName}" \
                    "${ovaUrl}" \
                    "${stepParameters.esxiGateway}${stepParameters.ip}"
            """
        }
    }
}

