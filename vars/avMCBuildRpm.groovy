// =================================================
// Author: drizzt.xia@dell.com
// Description: Build Avamar MC RPM file
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
        stepName: "build-mc",
        workspace: checkParam(this, "workspace", config.workspace, true),
        rpmPath: checkParam(this, "rpmPath", config.rpmPath, true),
        verbose: true,
    ]

    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        def scriptBuildMCRpm = "script/av-build-mc-rpm.sh"
        sh """
            chmod +x ${scriptBuildMCRpm}
            ./${scriptBuildMCRpm} "${stepParameters.workspace}" "${stepParameters.rpmPath}/${currentBuild.displayName}"
        """
    }
}
