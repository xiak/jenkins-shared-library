// =================================================
// Author: drizzt.xia@dell.com
// Description: Run pipeline step
// =================================================


import com.dell.ap.ProjectConfig
import com.dell.ap.steps.Step
import groovy.transform.Field

@Field String STEP_NAME = getClass().getName()

def call(ProjectConfig pc) {
    def stepParameters = [
        stepName: "steps-run",
        projectConfig: pc,
        verbose: true,
    ]
    handlePipelineStepErrors (script: this, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        return {
            List<Step> stepsList = stepParameters.projectConfig.steps.steps
            showStepList(stepsList)
            stepsList.each { step ->
                // parallel run
                echo "[DEBUG]  Parallel Step: ${step.isParallelStep}; Parallel Number: ${step.parallel.size()}"
                if (step.isParallelStep && step.parallel.size() > 0 ) {
                    def pjs = step.parallel.collectEntries { stageName, s ->
                        def param = getParameters(s.jobParameters)
                        ["${stageName}" : {
                            stage("${stageName}") {
                                build job: "${s.jobName}", parameters: param
                            }
                        }]
                    }
                    parallel pjs.plus(([failFast: !step.skipError]))
                } else {
                    def param = getParameters(step.parameters)
                    stage(step.name) {
                        skipError(skip: step.skipError) {
                            build job: "${step.jobName}", parameters: param
                        }
                    }
                }
            }
        }
    }
}

def showStepList(steps) {
    for (def i=0; i < steps.size(); i++) {
        echo "[STEP] - ${i} - Name: ${steps[i].name} , skipError: ${steps[i].skipError}"
    }
}

def getParameters(p=[:]) {
    def param = []
    param = p.collect { k, v ->
        string(name: "${k}", value: "${v}");
    }
    return param
}
