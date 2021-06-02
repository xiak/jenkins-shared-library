// =================================================
// Author: drizzt.xia@dell.com
// Description: Avamar server deployment
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
        release: checkParam(this, "release", config.release, true),
        build: checkParam(this, "build", config.build, true),
        ip: checkParam(this, "ip", config.ip, true),
        force: checkParam(this, "force", config.force, true),
        password: checkParam(this, "password", config.password, true),
        depot: checkParam(this, "depot", config.depot, true),
        netPrefix: checkParam(this, "netPrefix", config.netPrefix, true),
        netDNS: checkParam(this, "netDNS", config.netDNS, true),
        netNTP: checkParam(this, "netNTP", config.netNTP, true),
        esxiGateway: checkParam(this, "esxiGateway", config.esxiGateway, true),
        verbose: true,
    ]
    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        def avBuildNumber
        def avBuildUrl
        println("Get AVE version and OVA URL")
        if (stepParameters.build == "latest") {
            // 最新版本
            avBuildNumber  = sh(returnStdout: true, script: "set +e; python3 script/av.py ave get version --depot=${stepParameters.depot} --release=${stepParameters.release} --verbose=0; set -e")
            avBuildUrl     = sh(returnStdout: true, script: "set +e; python3 script/av.py ave get url --depot=${stepParameters.depot} --release=${stepParameters.release} --verbose=0; set -e")
        } else {
            // 指定版本
            avBuildNumber = "${stepParameters.release}.${stepParameters.build}"
            avBuildUrl    = sh(returnStdout: true, script: "set +e; python3 script/av.py ave get url --depot=${stepParameters.depot} --release=${stepParameters.release} --build=${stepParameters.build} --verbose=0; set -e")
        }
        avBuildNumber = avBuildNumber.trim()
        avBuildUrl = avBuildUrl.trim()
        println("AVE version is ${avBuildNumber}, AVE OVA URL is ${avBuildUrl}")
        if (!avBuildNumber || !avBuildUrl) {
            script.error("[ERROR] Can't get build info (release: ${stepParameters.release}, build: ${stepParameters.build}) from depot (${stepParameters.depot}), please check if it is available")
        }

        // 设置 job 显示名称
        script.currentBuild.displayName = "${avBuildNumber}"

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
        currentBuild.description = "${userId}: ${stepParameters.ip}, depot: ${stepParameters.depot}"

        def av           = new Avamar(stepParameters.ip, avBuildNumber)
        def avOsVersion  = av.getOSVersion()
        def avFQDN       = av.getFQDN()
        def avGateway    = av.getGateway()
        println("Get Avamar support key")
        def avSupportKey = sh(returnStdout: true, script: "set +e; python3 script/av.py ave get support --release=${stepParameters.release} --depot=${stepParameters.depot} --verbose=0; set -e")
        avSupportKey = avSupportKey.trim()
        println("Support key is ${avSupportKey}")
        if (avSupportKey == null) {
            script.error("[ERROR] Can't get Avamar support key, please check av.py")
        }
        if (avOsVersion == null) {
            script.error("[ERROR] Can't get Avamar os version")
        }
        if (avFQDN == null) {
            script.error("[ERROR] Can't get Avamar FQDN")
        }
        if (avGateway == null) {
            script.error("[ERROR] Can't get Avamar gateway")
        }

        def scriptAvCheck = "script/av-check.sh"
        def scriptAvInstall = "script/av-install.sh"
        def scriptConfigAve = "script/av-config.sh"
        def rc = 1
        if (stepParameters.force == "false") {
            println("Determine if need to reinstall the Avamar server (${stepParameters.ip})")
            rc = sh(script: "chmod a+x ./${scriptAvCheck} && ./${scriptAvCheck} ${stepParameters.ip} root ${stepParameters.password} ${avBuildNumber} 5 1", returnStatus: true)
        }
        if ( rc != 0 ) {
            echo "Install Avamar from OVA template"
            lockResource(resource: "avserver-${stepParameters.ip}") {
                sh """
                    chmod +x ./${scriptAvInstall}
                    # NOTE: 双斜线后不能有空格
                    ./${scriptAvInstall} \\
                        "${stepParameters.ip}" \\
                        "${stepParameters.netPrefix}" \\
                        "${avFQDN}" \\
                        "${avGateway}" \\
                        "${stepParameters.netDNS}" \\
                        "${stepParameters.netNTP}" \\
                        "av.server.${stepParameters.ip}" \\
                        "${stepParameters.esxiGateway}${stepParameters.ip}" \\
                        "${avBuildUrl}" \\
                        "${avBuildNumber}"
                """
                echo "Config Avamar ${stepParameters.ip}"
                sh """
                    chmod +x ${scriptConfigAve}
                    ./${scriptConfigAve} "${stepParameters.ip}" "${stepParameters.password}" "${avBuildNumber}" "${avSupportKey}" "${stepParameters.netNTP}" 800
                """
            }
        }
    }
}
