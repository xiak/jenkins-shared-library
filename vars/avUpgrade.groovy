// =================================================
// Author: drizzt.xia@dell.com
// Description: Upgrade Avamar server
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
        avServer: checkParam(this, "avServer", config.avServer, true),
        avServerUser: checkParam(this, "avServerUser", config.avServerUser, true),
        avServerPass: checkParam(this, "avServerPass", config.avServerPass, true),
        upgradeTo: checkParam(this, "upgradeTo", config.upgradeTo, true),
        depot: config?.depot ?: 'irvine',
        verbose: true,
    ]

    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        lockResource(resource: "avserver-${stepParameters.avServer}") {
            def scriptPasswordless = "script/passwordless.sh"
            def scriptAv = "script/av.py"
            def scriptUpgradeAve = "script/av-up.sh"
            def avVersion191 = "19.1.0.38"
            def versions = stepParameters.upgradeTo.split("\\.")
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
            println("Get upgrade version on QADepot")
            def upVersion = sh(returnStdout: true, script: "set +e; python3 ${scriptAv} avp get version --depot=${stepParameters.depot} --release=${upgrade2Release} ${buildParameter} --verbose=0; set -e")
            upVersion = "${upVersion}".trim()
            if (!upVersion) {
                message("err", "build ${upgrade2Release}.${buildParameter}) is not available on depot, please check manually")
            }
            println("Find version ${upVersion} on QADepot")

            // get version from avamar server via cmd: avmaint --version
            println("Get version from Avamar server ${stepParameters.avServer}")
            def version = sh(returnStdout: true, script: """ssh ${stepParameters.avServerUser}@${stepParameters.avServer} avmaint --version | head -1 | awk '{print \$2}' | sed s/-/./g""")
            version = "${version}".trim()
            println("Avamar server version is ${version}")
            if (version == "") {
                message("err", "cannot get version from Avamar server")
            }
            // 比较 Avamar server 版本和升级包的版本
            println("Compare Avamar server version ${version} and upgrade version ${upVersion}")
            def bigVersion = sh(returnStdout: true, script: "set +e; python3 ${scriptAv} ave compare version --version1=${upVersion} --version2=${version} --verbose=0; set -e")
            bigVersion = "${bigVersion}".trim()
            // 比较结果为空表示版本号相同
            if (bigVersion == "") {
                message("err", "Skip upgrade, because the version ${version} of Avamar server ${stepParameters.avServer} is equal to you wanted ${upVersion}")
            }

            // 检查 Avamar server version 是否小于 19.1
            println("Compare Avamar server version ${version} and ${avVersion191}")
            def bigAvAnd191 = sh(returnStdout: true, script: "set +e; python3 ${scriptAv} ave compare version --version1=${avVersion191} --version2=${version} --verbose=0; set -e")
            bigAvAnd191 = "${bigAvAnd191}".trim()
            // 如果 Avamar version 小于 19.1
            if ("${bigAvAnd191}" == "${avVersion191}") {
                println("Avamar server version ${version} is less than ${avVersion191}")
                // 如果升级包版本大于 19.1
                def bigUpAnd191 = sh(returnStdout: true, script: "set +e; python3 ${scriptAv} ave compare version --version1=${avVersion191} --version2=${upVersion} --verbose=0; set -e")
                bigUpAnd191 = "${bigUpAnd191}".trim()
                if ("${bigUpAnd191}" == "${upVersion}") {
                    message("err", "Cannot upgrade Avamar server from ${version} to ${upVersion}, you must upgrade Avamar to 19.1.0 first")
                }
                println("Upgrade package version ${upVersion} is less than ${avVersion191}")
            } else {
                println("Avamar server version ${version} is greater than ${avVersion191}")
            }

            // 升级包版本必须大于 Avamar server 现有的版本
            if ("${bigVersion}" == "${upVersion}") {
                message("Avamar server ${stepParameters.avServer} will be upgraded from ${version} to ${bigVersion}")
                // get Avamar server support key (only hardware installation need supportkey)
                // def supportKey = sh(returnStdout: true, script: "python3 ${scriptAv} ave get support --depot=${depot} --release=${version} --verbose=0")
                // supportKey = supportKey.trim()
                // 从 QADepot 获取 upgrade 包的下载链接
                println("Get avp url from depot ${stepParameters.depot}")
                def avpUrl = sh(returnStdout: true, script: "set +e; python3 ${scriptAv} avp get url --depot=${stepParameters.depot} --release=${upgrade2Release} ${buildParameter} --verbose=0; set -e")
                avpUrl = avpUrl.trim()
                println("The upgrade avp url is ${avpUrl}")
                println("Start to upgrade Avamar server")
                sh """
                    chmod a+x ${scriptUpgradeAve}
                    ./${scriptUpgradeAve} ${stepParameters.avServer} ${stepParameters.avServerUser} ${stepParameters.avServerPass} ${avpUrl} "null"
                """
            } else {
                message("err", "Skip upgrade, because the the version of Avamar server ${stepParameters.avServer} is greater than you wanted ${upVersion}")
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
