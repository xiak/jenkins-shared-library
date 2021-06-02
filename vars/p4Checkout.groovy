// =================================================
// Author: drizzt.xia@dell.com
// Description: SCM - perforce
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
        stepName: "scm-git",
        change: checkParam(this, "change", config.change, true),
        view: checkParam(this, "view", config.view, true),
        credentialsId: checkParam(this, "gitCredentialId", config.gitCredentialId, true),
        changelog: ("${config?.changelog}" == "true"),
        poll: ("${config?.poll}" == "true"),
        verbose: true,
    ]

    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        checkout changelog: stepParameters.changelog, poll: stepParameters.poll, scm: perforce(
            credential: "${stepParameters.credentialsId}",
            populate: autoClean(
                delete: true,
                modtime: false,
                parallel: [
                    enable: false,
                    minbytes: '1024',
                    minfiles: '1',
                    threads: '4'
                ],
                pin: stepParameters.change,
                quiet: true,
                replace: true,
                tidy: false
            ),
            workspace: manualSpec(
                charset: 'none',
                cleanup: false,
                name: "jenkins-${NODE_NAME}-${JOB_NAME}-${EXECUTOR_NUMBER}",
                pinHost: false,
                spec: clientSpec(
                    allwrite: false,
                    backup: true,
                    changeView: '',
                    clobber: true,
                    compress: false,
                    line: 'LOCAL',
                    locked: false,
                    modtime: false,
                    rmdir: false,
                    serverID: '',
                    streamName: '',
                    type: 'WRITABLE',
                    view: stepParameters.view
                )
            )
        )
    }

}