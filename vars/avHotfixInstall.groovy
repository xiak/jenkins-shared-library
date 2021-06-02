// =================================================
// Author: drizzt.xia@dell.com
// Description: Avamar hotfix installation
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
        stepName: "upgrade-avamar-server",
        bug: checkParam(this, "bug", config.bug, true),
        avServer: checkParam(this, "avServer", config.avServer, true),
        avServerUser: checkParam(this, "avServerUser", config.avServerUser, true),
        avServerPass: checkParam(this, "avServerPass", config.avServerPass, true),
        component: checkParam(this, "component", config.component, true),
        branch: checkParam(this, "branch", config.branch, true),
        depot: config?.depot ?: 'irvine',
        verbose: true,
    ]

    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        lockResource(resource: "avserver-${stepParameters.avServer}") {
            def scriptPasswordless = "script/passwordless.sh"
            def scriptAv = "script/av.py"
            def scriptAvUpdate = "script/av-up.sh"
            def avVersion191 = "19.1.0.38"
            def versions = stepParameters.branch.split("\\.")
            def upgrade2Release = "${versions[0]}.${versions[1]}.${versions[2]}"
            def upgrade2Build
            def buildParameter = ""
            if (versions.size() > 3) {
                upgrade2Build = versions[3]
                buildParameter = "--build ${upgrade2Build}"
            }
            currentBuild.displayName = stepParameters.avServer

            sh """
                chmod a+x ${scriptPasswordless};
                ./${scriptPasswordless} ${stepParameters.avServer} ${stepParameters.avServerUser} ${stepParameters.avServerPass};
            """
            // get version from qadpot, e.g.. 19.2.0.155
            println("Check hotfix version on QADepot")
            def hotfixVersion = sh(returnStdout: true, script: "set +e; python3 ${scriptAv} avp get version --depot=${stepParameters.depot} --release=${upgrade2Release} ${buildParameter} --verbose=0; set -e")
            hotfixVersion = "${hotfixVersion}".trim()
            if (!hotfixVersion) {
                message("err", "Cannot get version ${upgrade2Release}.${buildParameter}) from QADepot, please check if it is existed")
            }
            def hotfixUrl = "http://antimatter.asl.lab.emc.com/HOTFIXES/v${hotfixVersion}/${component}/${stepParameters.bug}"
            println("Found version ${hotfixVersion} on QADepot")
            // get version from avamar server via cmd: avmaint --version
            println("Get version from Avamar server ${stepParameters.avServer}")
            def version = sh(returnStdout: true, script: "ssh ${stepParameters.avServerUser}@${stepParameters.avServer} avmaint --version | head -1 | awk '{print \$2}' | sed s/-/./g")
            version = "${version}".trim()
            println("Avamar server version is ${version}")

            // 比较 Avamar servr version 是否小于 19.1
            def biggerAvAnd191 = sh(returnStdout: true, script: "set +e; python3 ${scriptAv} ave compare version --version1=${avVersion191} --version2=${version} --verbose=0; set -e")
            biggerAvAnd191 = "${biggerAvAnd191}".trim()
            // 如果 Avamar version 小于 19.1
            if ("${biggerAvAnd191}" == "${avVersion191}") {
                // 如果 Hotfix 版本大于 19.1.0.38
                def biggerHfAnd191 = sh(returnStdout: true, script: "set +e; python3 ${scriptAv} ave compare version --version1=${avVersion191} --version2=${hotfixVersion} --verbose=0; set -e")
                biggerHfAnd191 = "${biggerHfAnd191}".trim()
                if ( "${biggerHfAnd191}" == "${hotfixVersion}" ) {
                    message("err", "Avamar server version is ${version} and hotfix branch is ${branch}, you must upgrade Avamar to 19.1.0 first")
                }
            }
            def bigVersion = sh(returnStdout: true, script: "set +e; python3 ${scriptAv} ave compare version --version1=${hotfixVersion} --version2=${version} --verbose=0; set -e")
            bigVersion = "${bigVersion}".trim()
            // 比较结果为空表示版本号相同, 表示普通的 hotfix 安装
            if ("${bigVersion}" == "") {
                message("Install hotfix - bug:${bug}, branch:${hotfixVersion}")
                sh """
                    chmod a+x ${scriptAvUpdate}
                    ./${scriptAvUpdate} ${stepParameters.avServer} ${stepParameters.avServerUser} ${stepParameters.avServerPass} "null" "${hotfixUrl}"
                """
                // 如果 hotfix 版本大于 Avamar 版本, 表示 callable 安装 hotfix
            } else if ("${bigVersion}" == "${hotfixVersion}") {
                message("Callable install hotfix - bug:${bug}, branch:${hotfixVersion}")
                // 从 QADepot 获取 upgrade 包的下载链接
                def avpUrl = sh(returnStdout: true, script: "set +e; python3 ${scriptAv} avp get url --depot=${stepParameters.depot} --release=${upgrade2Release} ${buildParameter} --verbose=0; set -e")
                avpUrl = avpUrl.trim()
                if (!avpUrl) {
                    message("err", "Cannot get hotfix package from ${stepParameters.depot} depot")
                }
                sh """
                        chmod a+x ${scriptAvUpdate}
                        ./${scriptAvUpdate} ${stepParameters.avServer} ${stepParameters.avServerUser} ${stepParameters.avServerPass} "${avpUrl}" "${hotfixUrl}"
                    """
            } else {
                message("err", "Skip upgrade, because the the version of Avamar server ${stepParameters.avServer} is greater than you wanted ${stepParameters.branch}")
            }
        }
    }
}

def message(lvl="info", msg) {
    println(msg)
    currentBuild.description = msg
    if (lvl=="err") {
        this.error(msg)
    }
}
