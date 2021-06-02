// =================================================
// Author: drizzt.xia@dell.com
// Description: Check pipeline.script and parameters
// =================================================


package com.dell.ap

static checkScript(def step, Map params, Boolean prerequisite = false) {

    def script = params?.script

    if(script == null) {

        if(prerequisite) {
            step.error("[ERROR][${step.STEP_NAME}] No reference to surrounding script provided with key 'script', e.g. 'script: this'.")
        }

        step.currentBuild.setResult('UNSTABLE')

        step.echo "[WARNING][${step.STEP_NAME}] No reference to surrounding script provided with key 'script', e.g. 'script: this'. " +
            "Build status has been set to 'UNSTABLE'. In future versions of piper-lib the build will fail."
    }

    return script
}

static checkParam(def step, paramName = "", param, Boolean prerequisite = false) {
    if(param == null) {

        if(prerequisite) {
            step.error("[ERROR][${step.STEP_NAME}.groovy] Param missing: ${paramName}")
        }

        step.currentBuild.setResult('UNSTABLE')

        step.echo "[WARNING][${step.STEP_NAME}] You check this param but missing: ${paramName}"
    }

    return param
}
