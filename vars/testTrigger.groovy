// =================================================
// Author: drizzt.xia@dell.com
// Description: Test trigger
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
        depot: checkParam(this, "depot", config.depot, true),
        force: checkParam(this, "force", config.force, true),
        release: checkParam(this, "release", config.release, true),
        markerPath: checkParam(this, "markerPath", config.markerPath, true),
        verbose: true,
    ]

    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        depot = depot.toLowerCase()
        echo "Check if there is an new build on ${stepParameters.depot} depot"
        def clean = ""
        if (stepParameters.force == "true" || stepParameters.force == "yes") {
            clean = "--force"
        }
        sh "mkdir -p '${stepParameters.markerPath}'"
        def triggered = sh(returnStatus: true, script: "python3 script/av.py ave trigger version --release=${stepParameters.release} --depot=${stepParameters.depot} --flag=${stepParameters.markerPath} --verbose=4 ${clean}")
        if (triggered == 0) {
            currentBuild.displayName = "Triggered"
            def avBuildVersion  = sh(returnStdout: true, script: "set +e; python3 script/av.py ave get version --depot=${stepParameters.depot} --release=${stepParameters.release} --verbose=0; set -e")
            // 19.3.0.149
            avBuildVersion = avBuildVersion.trim()
            // 149
            avBuildNumber = avBuildVersion.split("\\.")[-1].trim()
            currentBuild.result = "SUCCESS"
            echo "AVE version is ${avBuildVersion} and build number is ${avBuildNumber}"
            currentBuild.description = "build: ${avBuildVersion}, force: ${stepParameters.force}"
        } else {
            currentBuild.displayName = "Not triggered"
            currentBuild.result = "UNSTABLE"
            echo "Not triggered, no new build found on on ${stepParameters.depot} depot"
        }
    }

}
