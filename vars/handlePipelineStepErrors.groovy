// =================================================
// Author: drizzt.xia@dell.com
// Description: Handle all errors of pipeline
// =================================================


import groovy.text.GStringTemplateEngine
import groovy.transform.Field
import hudson.AbortException
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

@Field STEP_NAME = getClass().getName()


void call(Map parameters = [:], body) {
    String stageName = parameters.stepParameters?.stageName ?: env.STAGE_NAME
    String stepName = parameters.stepParameters?.stepName ?: STEP_NAME

    def message = ''
    try {
        if (parameters?.echoDetails)
            echo "--- Begin jenkins library step of: ${stepName} ---"
        if (!parameters?.failOnError && parameters?.stepTimeout) {
            timeout(time: parameters?.stepTimeout) {
                body()
            }
        } else {
            body()
        }
    } catch (AbortException | FlowInterruptedException ex) {
        if (parameters?.echoDetails) {
            message += fmtErrMessage(parameters, ex)
        }
        if (parameters?.failOnError) {
            throw ex
        }

        def failureMessage = "Stage [${stageName}] Error in step [${stepName}] - Build result set to 'UNSTABLE'"
        try {
            //use new unstable feature if available: see https://jenkins.io/blog/2019/07/05/jenkins-pipeline-stage-result-visualization-improvements/
            unstable(failureMessage)
        } catch (java.lang.NoSuchMethodError nmEx) {
            if (parameters?.script) {
                parameters?.script.currentBuild.result = 'UNSTABLE'
            } else {
                currentBuild.result = 'UNSTABLE'
            }
            echo failureMessage
        }
    } catch (Throwable error) {
        if (parameters?.echoDetails) {
            message += fmtErrMessage(parameters, error)
        }
        throw error
    } finally {
        if (parameters?.echoDetails) {
            message += "--- End jenkins library step of: ${stepName} ---"
        }
        echo message
    }
}

@NonCPS
private String fmtErrMessage(Map config, error){
    Map binding = [
        error: error,
        stepName: config.stepParameters?.stepName ?: STEP_NAME,
        stepParameters: (config.stepParameters?.verbose == true) ? config.stepParameters?.toString() : '*** to show step parameters, set verbose:true in general pipeline configuration\n*** WARNING: this may reveal sensitive information. ***'
    ]
    return GStringTemplateEngine
        .newInstance()
        .createTemplate(libraryResource('com.dell.ap/templates/error.tmpl'))
        .make(binding)
        .toString()
}
