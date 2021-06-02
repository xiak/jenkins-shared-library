// =================================================
// Author: drizzt.xia@dell.com
// Description: Send mail
// =================================================


import static com.dell.ap.Precheck.checkParam

import groovy.transform.Field

@Field String STEP_NAME = getClass().getName()

void call(Map parameters = [:]) {

    def stepParameters = [
        stepName: "send-mail",
        // 必填
        subject: checkParam(this, "subject", parameters.subject, true),
        content: checkParam(this, "content", parameters.content, true),
        // 可选
        mimeType: parameters?.mimeType ?: 'text/html',
        mailRecipients: parameters?.mailRecipients ?: '',
        attachLog: (parameters?.attachLog == true) ? true : false,
        verbose: true,
    ]
    handlePipelineStepErrors (script: this, stepParameters: stepParameters, failOnError: true, echoDetails: true) {
        emailext(
            mimeType: stepParameters.mimeType,
            subject: stepParameters.subject.trim(),
            body: stepParameters.content,
            to: stepParameters.mailRecipients.trim(),
            recipientProviders: [requestor(), culprits(), developers()],
            attachLog: stepParameters.attachLog,
        )
    }
}
