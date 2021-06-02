// =================================================
// Author: drizzt.xia@dell.com
// Description: Build go binary
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
        stepName: "build-golang-container-image",
        binName: checkParam(this, "binName", config.binName, true),
        binPath: config?.binPath ?: '.',
        mainPath: config?.mainPath ?: '',
        workDir: config?.workDir ?: '.',
        vendor: ("${config?.vendor}" == "true"),
        verbose: true,
    ]
    handlePipelineStepErrors (script: script, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        def vendor = ""
        if ( "${stepParameters.vendor}" == "true" ) {
            vendor = " -mod=vendor "
        }
        sh """
            cd ${stepParameters.workDir}
            go build ${vendor} -o ${stepParameters.binPath}/${stepParameters.binName} ${stepParameters.mainPath}
            chmod a+x ${stepParameters.binPath}/${stepParameters.binName}
            ls -l ${stepParameters.binPath}/${stepParameters.binName}
        """
    }
}
