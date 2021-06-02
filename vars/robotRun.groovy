// =================================================
// Author: drizzt.xia@dell.com
// Description: run robot test
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
        stepName: "robot-run",
        command: checkParam(this, "command", config.command, true),
        logPath: checkParam(this, "logPath", config.logPath, true),
        configFile: config.configFile ?: '',
        configFileModify: config?.configFileModify ?: [:],
        verbose: true,
    ]

    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        // 替换 robot resource file 中的内容
        stepParameters.configFileModify.each { k, v ->
            sh "sed -i 's#${k}#${v}#' ${stepParameters.configFile}"
        }

        // 执行命令
        def rc = sh(returnStatus: true, script: stepParameters.command)
        print("Task return code: ${rc}")

        def workspace = pwd()
        def filePath = "${workspace}/output.xml"
        def outputIsExisted = fileExists(filePath)
        if (outputIsExisted) {
            script.env.isRobotTask = "true"
        } else {
            script.env.isRobotTask = "false"
        }
        print("Output files ${filePath} is existed: ${script.env.isRobotTask}")

        // 如果指定 logPath, 则 log 会保存到 logPath 中
        if (stepParameters.logPath && outputIsExisted) {
            def jobName = "${env.JOB_NAME}"
            def buildNo = "${env.BUILD_NUMBER}"
            jobName = jobName.replace("#", "").replace("/", "-")
            buildNo = buildNo.replace("#", "").replace("/", "-")
            logPath = "${stepParameters.logPath}/${jobName}-${buildNo}"
            println("Put logs to path ${logPath}")
            sh """
                mkdir -p '${logPath}'
                cp output.xml '${logPath}/output.xml'
                cp log.html '${logPath}/log.html'
                cp report.html '${logPath}/report.html'
            """
        }
    }
}

