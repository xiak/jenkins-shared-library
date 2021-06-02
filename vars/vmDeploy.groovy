// =================================================
// Author: drizzt.xia@dell.com
// Description: Deploy VM
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
        ip: checkParam(this, "ip", config.ip, true),
        domain: checkParam(this, "domain", config.domain, true),
        password: checkParam(this, "password", config.password, true),
        netPrefix: checkParam(this, "netPrefix", config.netPrefix, true),
        netDNS: checkParam(this, "netDNS", config.netDNS, true),
        netMask: checkParam(this, "netMask", config.netMask, true),
        netNTP: checkParam(this, "netNTP", config.netNTP, true),
        vmTemplate: checkParam(this, "vmTemplate", config.vmTemplate, true),
        vmTemplatePassword: checkParam(this, "vmTemplatePassword", config.vmTemplatePassword, true),
        esxiGateway: checkParam(this, "esxiGateway", config.esxiGateway, true),
        osType: checkParam(this, "osType", config.osType, true),
        timezone: config.timezone,
        verbose: true,
    ]
    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        echo "Install Avamar client from template"
        def av = new Avamar(stepParameters.ip, "", "avmc-_R1_-_R2_.drm.lab.emc.com")
        def netGateway = av.getGateway()
        def hostname = av.getHostname()
        if (netGateway == null) {
            script.error("[ERROR] Can't get Avamar gateway")
        }
        if (hostname == null) {
            script.error("[ERROR] Can't get Avamar client hostname")
        }
        currentBuild.displayName = stepParameters.ip
        // 设置 job 描述名称, 需要安装插件 build user var plugin
        // plugin 地址: https://plugins.jenkins.io/build-user-vars-plugin/
        def userId
        wrap([$class: 'BuildUser']) {
            if (env.BUILD_USER_ID) {
                userId = env.BUILD_USER_ID
            } else {
                userId = "timer"
            }
        }
        currentBuild.description = "Reserved by ${userId}"

        lockResource(resource: "avclient-${stepParameters.ip}") {
            def scriptVmInstall = "script/vm-install.sh"
            sh """
                chmod +x ${scriptVmInstall}
                # 安装 vm
                # NOTE: 双斜线后不能有空格
                ./${scriptVmInstall} \\
                    "${stepParameters.ip}" \\
                    "${hostname}" \\
                    "${stepParameters.domain}" \\
                    "${stepParameters.password}" \\
                    "${stepParameters.netPrefix}" \\
                    "${stepParameters.netMask}" \\
                    "${stepParameters.netDNS}" \\
                    "${stepParameters.netNTP}" \\
                    "${netGateway}" \\
                    "av.client.${stepParameters.ip}" \\
                    "${stepParameters.osType}" \\
                    "${stepParameters.vmTemplate}" \\
                    "${stepParameters.vmTemplatePassword}" \\
                    "${stepParameters.esxiGateway}${stepParameters.ip}" \\
                    "${stepParameters.timezone}"
            """
        }
    }
}
